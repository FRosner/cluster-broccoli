package de.frosner.broccoli.nomad

import cats.instances.future._
import cats.data.EitherT
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
class NomadHttpClient(baseUrl: String, client: WSClient)(implicit context: ExecutionContext) extends NomadClient {

  /**
    * Get allocations for a job.
    *
    * @param jobId The ID of the job
    * @return The list of allocations for the job
    */
  override def getAllocationsForJob(jobId: String @@ Job.Id): Future[WithId[immutable.Seq[Allocation]]] =
    for {
      response <- url(s"job/$jobId/allocations")
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
        url(s"allocation/$id")
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
          url(s"node/${allocation.nodeId}")
            .withHeaders(ACCEPT -> JSON)
            .get())
        .ensureOr(toError)(_.status == OK)
        .map(_.json.as[Node])
      log <- EitherT
        .right(
          client
            .url(s"${allocationNode.httpAddress}/v1/client/fs/logs/$allocationId")
            .withQueryString(
              "task" -> taskName,
              "type" -> stream.entryName,
              // Request the plain text log without frameing and do not follow the log
              "plain" -> "true",
              "follow" -> "false"
            )
            .withHeaders(ACCEPT -> TEXT)
            .get())
        .ensureOr(toError)(_.status == OK)
    } yield TaskLog(stream, tag[TaskLog.Contents](log.body))

  /**
    * Build a web request with the given API path (without /v1/ prefix).
    *
    * @param apiPath The API path for API v1 (without /v1/ prefix)
    * @return The corresponding web request.
    */
  private def url(apiPath: String): WSRequest = client.url(s"$baseUrl/v1/$apiPath")

  private def toError(response: WSResponse): NomadError = response.status match {
    case NOT_FOUND => NomadError.NotFound
    // For unexpected errors throw an exception instead to trigger logging
    case _ => throw new UnexpectedNomadHttpApiError(response)
  }
}
