package de.frosner.broccoli.nomad

import de.frosner.broccoli.nomad.models.{Allocation, WithId}
import play.api.libs.ws.{WSClient, WSRequest}

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
  override def getAllocationsForJob(jobId: String): Future[WithId[immutable.Seq[Allocation]]] =
    for {
      response <- url(s"job/$jobId/allocations")
        .withHeaders("Accept" -> "application/json")
        .get()
    } yield WithId(jobId, response.json.as[immutable.Seq[Allocation]])

  /**
    * Build a web request with the given API path (without /v1/ prefix).
    *
    * @param apiPath The API path for API v1 (without /v1/ prefix)
    * @return The corresponding web request.
    */
  private def url(apiPath: String): WSRequest = client.url(s"$baseUrl/v1/$apiPath")
}
