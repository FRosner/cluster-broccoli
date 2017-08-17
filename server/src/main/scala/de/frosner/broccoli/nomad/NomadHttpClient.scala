package de.frosner.broccoli.nomad

import cats.instances.future._
import cats.data.EitherT
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import de.frosner.broccoli.nomad.models._
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.http.MimeTypes.{JSON, TEXT}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import shapeless.tag
import shapeless.tag.@@

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * A client for the HTTP API of Nomad.
  */
class NomadHttpClient(baseUri: Uri, client: WSClient)(implicit context: ExecutionContext) extends NomadClient {

  /**
    * The base URI for the Nomad V1 HTTP API
    */
  private val v1: Uri = baseUri / "v1"

  /**
    * Get allocations for a job.
    *
    * @param jobId The ID of the job
    * @return The list of allocations for the job
    */
  override def getAllocationsForJob(jobId: String @@ Job.Id): Future[WithId[immutable.Seq[Allocation]]] =
    for {
      response <- client
        .url(v1 / "job" / jobId / "allocations")
        .withHeaders(ACCEPT -> JSON)
        .get()
    } yield WithId(jobId, response.json.as[immutable.Seq[Allocation]])

  /**
    * Get an allocation.
    *
    * @param id The alloction to query
    * @return The allocation or an error
    */
  override def getAllocation(id: String @@ Allocation.Id): EitherT[Future, NomadError, Allocation] =
    EitherT
      .right(
        client
          .url(v1 / "allocation" / id)
          .withHeaders(ACCEPT -> JSON)
          .get())
      .ensureOr(toError)(_.status == OK)
      .map(_.json.as[Allocation])

  /**
    * Get the log of a task on an allocation.
    *
    * Nomad provides logs of tasks, but getting hold of these involves a few requests as Nomad only exposes logs as part
    * of the client API.  This API has an important restriction: We must request those endpoints from the node that runs
    * the allocation of interest.  We cannot simply ask any other Nomad server, ie, the one Broccoli uses for everything
    * else.
    *
    * So we
    *
    * - request the allocation to find out the ID of the node it runs on,
    * - request information about the node with that ID to get the HTTP API address of the node,
    * - and finally ask that address for the logs.
    *
    * @param allocationId The ID of the allocation
    * @param taskName     The name of the task
    * @param stream       The kind of log to fetch
    * @return The task log
    */
  override def getTaskLog(
      allocationId: String @@ Allocation.Id,
      taskName: String @@ Task.Name,
      stream: LogStreamKind
  ): EitherT[Future, NomadError, TaskLog] =
    for {
      allocation <- getAllocation(allocationId)
      allocationNode <- EitherT
        .right(
          client
            .url(v1 / "node" / allocation.nodeId)
            .withHeaders(ACCEPT -> JSON)
            .get())
        .ensureOr(toError)(_.status == OK)
        .map(_.json.as[Node])
      nodeAddress = parseNodeAddress(allocationNode.httpAddress)
      log <- EitherT
        .right(
          client
            .url(v1.copy(host = nodeAddress.host, port = nodeAddress.port) / "client" / "fs" / "logs" / allocationId)
            .withQueryString(
              "task" -> taskName,
              "type" -> stream.entryName,
              // Request the plain text log without framing and do not follow the log
              "plain" -> "true",
              "follow" -> "false"
            )
            .withHeaders(ACCEPT -> TEXT)
            .get())
        .ensureOr(toError)(_.status == OK)
    } yield TaskLog(stream, tag[TaskLog.Contents](log.body))

  private def toError(response: WSResponse): NomadError = response.status match {
    case NOT_FOUND => NomadError.NotFound
    // For unexpected errors throw an exception instead to trigger logging
    case _ => throw new UnexpectedNomadHttpApiError(response)
  }

  /**
    * Parse the HTTP address of a node into a partial URI
    *
    * @param httpAddress The HTTP address
    * @return The partial URI
    */
  private def parseNodeAddress(httpAddress: String @@ Node.HttpAddress): Uri = httpAddress.split(":", 2) match {
    case Array(host, port, _*) => Uri().withHost(host).withPort(port.toInt)
    case Array(host)           => Uri().withHost(host)
    case _                     => Uri()
  }

}
