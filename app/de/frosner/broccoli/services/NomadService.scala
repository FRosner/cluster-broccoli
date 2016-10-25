package de.frosner.broccoli.services

import javax.inject.{Inject, Named}

import akka.actor._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus}
import de.frosner.broccoli.services.ConsulService.ServiceStatusRequest
import de.frosner.broccoli.services.InstanceService.{NomadNotReachable, NomadStatuses}
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success}

class NomadService @Inject()(configuration: Configuration,
                             @Named("consul-actor") consulActor: ActorRef,
                             ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString(conf.NOMAD_URL_KEY).getOrElse(conf.NOMAD_URL_DEFAULT)
  private val nomadJobPrefix = configuration.getString(conf.NOMAD_JOB_PREFIX_KEY).getOrElse(conf.NOMAD_JOB_PREFIX_DEFAULT)

  def receive = {
    case GetStatuses => executeGetStatuses()
    case GetServices(id) => executeGetServices(id)
    case StartJob(job) => executeStartJob(job)
    case DeleteJob(id) => executeDeleteJob(id)
  }

  private[this] def executeGetStatuses() = {
    val sendingService = sender()
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val jobsRequest = ws.url(queryUrl).withQueryString("prefix" -> nomadJobPrefix)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, statuses) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Status").map(_.as[JsString].value))
      (ids, statuses)
    })
    jobsWithTemplate.onComplete{
      case Success((ids, statuses)) => {
        Logger.debug(s"${jobsRequest.uri} => ${ids.zip(statuses).mkString(", ")}")
        val idsAndStatuses = ids.zip(statuses.map {
          case "running" => InstanceStatus.Running
          case "pending" => InstanceStatus.Pending
          case "dead" => InstanceStatus.Dead
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

  private[this] def executeGetServices(id: String) = {
    val sendingService = sender()
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val jobRequest = ws.url(queryUrl)
    val jobResponse = jobRequest.get().map(_.json.as[JsObject])
    val eventuallyJobServiceIds = jobResponse.map{ jsObject =>
      val services = (jsObject \\ "Services").flatMap(_.as[JsArray].value.map(_.as[JsObject]))
      services.flatMap {
        serviceJsObject => serviceJsObject.value.get("Name").map(_.as[JsString].value)
      }
    }
    eventuallyJobServiceIds.onComplete {
      case Success(jobServiceIds) =>
        Logger.debug(s"${jobRequest.uri} => ${jobServiceIds.mkString(", ")}")
        consulActor.tell(ServiceStatusRequest(id, jobServiceIds), sendingService)
      case Failure(throwable) =>
        Logger.error(throwable.toString)
        sendingService ! NomadNotReachable
    }
  }

  private[this] def executeStartJob(job: JsValue) = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending job definition to ${request.uri}")
    request.post(job)
  }

  private[this] def executeDeleteJob(id: String) = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending deletion request to ${request.uri}")
    request.delete()
  }

}

object NomadService {
  case object GetStatuses
  case class GetServices(id: String)
  case class StartJob(job: JsValue)
  case class DeleteJob(id: String)
}

