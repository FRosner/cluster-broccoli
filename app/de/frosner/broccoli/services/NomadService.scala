package de.frosner.broccoli.services

import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.models.{Instance, InstanceStatus}
import de.frosner.broccoli.services.InstanceService.{NomadNotReachable, NomadStatuses}
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json.{JsArray, JsString, JsValue}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success}

class NomadService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  def receive = {
    case GetStatuses => executeGetStatuses()
    case StartJob(job) => executeStartJob(job)
    case DeleteJob(id) => executeDeleteJob(id)
  }

  private[this] def executeGetStatuses() = {
    val sendingService = sender()
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val jobsRequest = ws.url(queryUrl).withQueryString("prefix" -> nomadJobPrefix)
    Logger.info(s"Requesting job status update (${jobsRequest.uri})")
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, statuses) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Status").map(_.as[JsString].value))
      (ids, statuses)
    })
    jobsWithTemplate.onComplete{
      case Success((ids, statuses)) => {
        Logger.debug(s"Received a status update of jobs: ${ids.mkString(", ")}")
        val idsAndStatuses = ids.zip(statuses.map {
          case "running" => InstanceStatus.Running
          case "pending" => InstanceStatus.Pending
          case default => Logger.warn(s"Unmatched status received: $default")
            InstanceStatus.Unknown
        })
        sendingService ! NomadStatuses(idsAndStatuses.toMap)
      }
      case Failure(throwable) => {
        Logger.error(throwable.toString)
        sendingService ! NomadNotReachable
      }
    }
  }

  private[this] def executeStartJob(job: JsValue) = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending job definition to ${request.uri}")
    request.post(job)
  }

  // TODO encourage to use prefix and ACLs or separate nomad cluster to avoid deletion of jobs that don't belong to the service
  private[this] def executeDeleteJob(id: String) = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending deletion request to ${request.uri}")
    request.delete()
  }

}

object NomadService {
  case object GetStatuses
  case class StartJob(job: JsValue)
  case class DeleteJob(id: String)
}

