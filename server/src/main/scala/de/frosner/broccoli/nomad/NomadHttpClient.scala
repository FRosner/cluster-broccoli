package de.frosner.broccoli.nomad

import java.net.ConnectException
import java.util.concurrent.TimeUnit

import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import io.lemonlabs.uri.Url
import io.lemonlabs.uri.dsl._
import de.frosner.broccoli.nomad.models._
import play.api.http.HeaderNames._
import play.api.http.MimeTypes.{JSON, TEXT}
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsString}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import shapeless.tag
import shapeless.tag.@@
import squants.Quantity
import squants.information.{Bytes, Information}

import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

/**
  * A client for the HTTP API of Nomad.
  */
class NomadHttpClient(
    baseUri: Url,
    tokenEnvName: String,
    client: WSClient
)(implicit override val executionContext: ExecutionContext)
    extends NomadClient {
  import NomadHttpClient._

  private val log = play.api.Logger(getClass)

  private class NodeClient(nodeV1Uri: Url) extends NomadNodeClient {

    private val v1Client: Url = nodeV1Uri / "client"

    /**
      * Get resource usage statistics of an allocation.
      *
      * If parsing allocation stats fails we treat it as a not-found allocation.
        This is done because Nomad might return something malformed while the allocation is still being built.
      *
      * @param allocationId The ID of the allocation
      * @return The resource statistics of the allocation with the given ID.
      */
    override def getAllocationStats(allocationId: @@[String, Allocation.Id],
                                    namespace: Option[String]): NomadT[AllocationStats] =
      lift(
        requestWithNamespace(
          requestWithHeaders(v1Client / "allocation" / allocationId / "stats").withHeaders(ACCEPT -> JSON),
          namespace).get())
        .subflatMap { response =>
          val maybeAllocationStats = for {
            responseJson <- Try(response.json).toOption
            allocationStats <- responseJson.validate[AllocationStats].asOpt
          } yield allocationStats
          maybeAllocationStats.toRight(NomadError.NotFound)
        }

    /**
      * Get the log of a task on an allocation.
      *
      * @param allocationId The ID of the allocation
      * @param taskName     The name of the task
      * @param stream       The kind of log to fetch
      * @return The task log
      */
    override def getTaskLog(
        allocationId: @@[String, Allocation.Id],
        taskName: @@[String, Task.Name],
        stream: LogStreamKind,
        offset: Option[@@[Quantity[Information], TaskLog.Offset]],
        namespace: Option[String]
    ): NomadT[TaskLog] =
      lift(
        requestWithNamespace(requestWithHeaders(v1Client / "fs" / "logs" / allocationId), namespace)
          .withQueryString(
            Seq("task" -> taskName,
                "type" -> stream.entryName,
                // Request the plain text log without framing and do not follow the log
                "plain" -> "true",
                "follow" -> "false") ++
              offset
                .map { size =>
                  Seq(
                    // If an offset is given, at the corresponding parameters to the query
                    "origin" -> "end",
                    // Round to nearest integer get the (double) value out, and convert it to an integral string
                    "offset" -> (size in Bytes).rint.value.toInt.toString
                  )
                }
                .getOrElse(Seq.empty): _*
          )
          .withHeaders(ACCEPT -> TEXT)
          .get())
        .ensureOr(fromClientHTTPError)(_.status == OK)
        .map(log => TaskLog(stream, tag[TaskLog.Contents](log.body)))

    /**
      * Create a Nomad error from a HTTP error response from the client API, ie, /v1/client/….
      *
      * As of Nomad 0.5.x this API does not return any semantic error codes; all errors map to status code 500, with some
      * information about the error in the plain text request body.
      *
      * This method tries and guess the cause of the error and turn the 500 into a reasonable NomadError.
      *
      * If you're not requesting the client API, use fromHTTPError!.
      *
      * @param response The response of the client API
      * @return A best guess NomadError corresponding to the response.
      */
    private def fromClientHTTPError(response: WSResponse): NomadError = response.status match {
      case INTERNAL_SERVER_ERROR if response.body.trim().startsWith("unknown allocation ID") => NomadError.NotFound
      case _                                                                                 => fromHTTPError(response)
    }
  }

  /**
    * The base URI for the Nomad V1 HTTP API
    */
  private val v1: Url = baseUri / "v1"

  /**
    * The AUTH headers required for nomad ACL
    */
  val headers: List[(String, String)] =
    sys.env.get(tokenEnvName).map(authToken => ("X-Nomad-Token", authToken)).toList

  /**
    * Nomad Version. Initiated lazily
    */
  override lazy val nomadVersion: String = getNomadVersion()
    .getOrElse {
      log.warn(s"Error fetching nomad version defaulting to $NOMAD_V_FOR_PARSE_API");
      NOMAD_V_FOR_PARSE_API
    }

  /**
    * Helper method that adds AUTH headers to request
    * @param url The url to request
    * @return
    */
  def requestWithHeaders(url: String): WSRequest =
    client.url(url).withHeaders(headers: _*)

  private def requestWithNamespace(request: WSRequest, maybeNamespace: Option[String]): WSRequest =
    maybeNamespace.map(namespace => request.withQueryString(("namespace", namespace))).getOrElse(request)

  /**
    * Get a job.
    *
    * @param jobId The ID of the job
    * @return The job
    */
  override def getJob(jobId: @@[String, Job.Id], namespace: Option[String]): NomadT[Job] =
    for {
      response <- lift(
        requestWithNamespace(requestWithHeaders(v1 / "job" / jobId).withHeaders(ACCEPT -> JSON), namespace).get())
        .ensureOr(fromHTTPError)(_.status == OK)
    } yield response.json.as[Job]

  /**
    * Get allocations for a job.
    *
    * @param jobId The ID of the job
    * @return The list of allocations for the job
    */
  override def getAllocationsForJob(jobId: String @@ Job.Id,
                                    namespace: Option[String]): NomadT[WithId[immutable.Seq[Allocation]]] =
    for {
      response <- lift(
        requestWithNamespace(requestWithHeaders(v1 / "job" / jobId / "allocations"), namespace)
          .withHeaders(ACCEPT -> JSON)
          .get())
        .ensureOr(fromHTTPError)(_.status == OK)
    } yield WithId(jobId, response.json.as[immutable.Seq[Allocation]])

  /**
    * Get an allocation.
    *
    * @param id The alloction to query
    * @return The allocation or an error
    */
  override def getAllocation(id: String @@ Allocation.Id, namespace: Option[String]): NomadT[Allocation] =
    lift(
      requestWithNamespace(requestWithHeaders(v1 / "allocation" / id), namespace)
        .withHeaders(ACCEPT -> JSON)
        .get())
      .ensureOr(fromHTTPError)(_.status == OK)
      .map(_.json.as[Allocation])

  override def getNode(id: @@[String, Node.Id]): NomadT[Node] =
    lift(requestWithHeaders(v1 / "node" / id).withHeaders(ACCEPT -> JSON).get())
      .ensureOr(fromHTTPError)(_.status == OK)
      .map(_.json.as[Node])

  /**
    * Get a client to access a specific Nomad node.
    *
    * @param node The node to access
    * @return A client to access the given node.
    */
  override def nodeClient(node: Node): NomadNodeClient = {
    val nodeAddress: Url = parseNodeAddress(node.httpAddress)
    nodeAddress.hostOption
      .map(host => nodeAddress.port.map(port => v1.withHost(host).withPort(port)).getOrElse(v1.withHost(host)))
      .map(new NodeClient(_))
      .getOrElse(new NodeClient(v1))
  }

  /**
    * Create a Nomad error from a HTTP error response.
    *
    * @param response The error response
    * @return The corresponding NomadError
    */
  private def fromHTTPError(response: WSResponse): NomadError = response.status match {
    case NOT_FOUND => NomadError.NotFound
    // For unexpected errors throw an exception instead to trigger logging
    case _ => throw new UnexpectedNomadHttpApiError(response)
  }

  private def getNomadVersion(): Option[String] = {
    val result = Await.result(requestWithHeaders(v1 / "agent" / "self")
                                .withHeaders(ACCEPT -> JSON)
                                .get(),
                              Duration(5, TimeUnit.SECONDS))
    val lookup = result.json \ "config" \ "Version"
    lookup.toOption match {
      case Some(JsString(s)) => Some(s)
      // For later versions the version information structure is nested
      case Some(JsObject(_)) => (lookup \ "Version").asOpt[String]
      case _                 => None
    }
  }

  /**
    * Parse the HTTP address of a node into a partial URI
    *
    * @param httpAddress The HTTP address
    * @return The partial URI
    */
  private def parseNodeAddress(httpAddress: String @@ Node.HttpAddress): Url = httpAddress.split(":", 2) match {
    case Array(host, port, _*) => Url().withHost(host).withPort(port.toInt)
    case Array(host)           => Url().withHost(host)
    case _                     => Url()
  }

  /**
    * Lift a WSResponse into the Nomad Either transformer.
    *
    * Transforms some exceptions into proper Nomad errors.
    *
    * @param response The response
    * @return The response in the nomad monad, with some exceptions caught.
    */
  private def lift(response: Future[WSResponse]): NomadT[WSResponse] =
    EitherT(response.map(_.asRight).recover {
      case _: ConnectException => NomadError.Unreachable.asLeft
    })
}

object NomadHttpClient {
  // The minimum version of nomad needed before /v1/jobs/parse works.
  // This is also the default version of nomad we use when the we can't fetch the version
  val NOMAD_V_FOR_PARSE_API = "0.8.2"
}
