package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named, Singleton}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.InstanceStatus._
import de.frosner.broccoli.models.InstanceStatus
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.libs.ws.WSClient
import play.api.Configuration

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@Singleton
class NomadService @Inject()(configuration: Configuration,
                             consulService: ConsulService,
                             ws: WSClient) {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString(conf.NOMAD_URL_KEY).getOrElse(conf.NOMAD_URL_DEFAULT)
  private val nomadJobPrefix = conf.getNomadJobPrefix(configuration)

  private val logger = play.api.Logger(this.getClass.getSimpleName)

  @volatile
  var jobStatuses: Map[String, InstanceStatus] = Map.empty

  def getJobStatusOrDefault(id: String): InstanceStatus = {
    if (nomadReachable) {
      jobStatuses.getOrElse(id, InstanceStatus.Stopped)
    } else {
      InstanceStatus.Unknown
    }
  }

  @volatile
  private var nomadReachable: Boolean = true

  def setNomadNotReachable() = {
    nomadReachable = false
    consulService.serviceStatuses = Map.empty
  }

  def setNomadReachable() = {
    nomadReachable = true
  }

  def requestStatuses() = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val jobsRequest = ws.url(queryUrl).withQueryString("prefix" -> nomadJobPrefix)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, statuses) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Status").map(_.as[JsString].value))
      (ids, statuses)
    })
    jobsWithTemplate.onComplete{
      case Success((ids, statuses)) => {
        logger.debug(s"${jobsRequest.uri} => ${ids.zip(statuses).mkString(", ")}")
        val idsAndStatuses = ids.zip(statuses.map {
          case "running" => InstanceStatus.Running
          case "pending" => InstanceStatus.Pending
          case "dead" => InstanceStatus.Dead
          case default => logger.warn(s"Unmatched status received: $default")
            InstanceStatus.Unknown
        })
        setNomadReachable()
        updateStatusesBasedOnNomad(idsAndStatuses.toMap)
      }
      case Failure(throwable) => {
        logger.error(throwable.toString)
        setNomadNotReachable()
      }
    }
  }

  // TODO should this go back to InstanceService or how? I mean I need to check the existing instances in order to know whether
  // TODO something is stopped (i.e. not present in Nomad). Or we use a cache that let's the stuff expire if you don't get an answer from Nomad
  // https://www.playframework.com/documentation/2.5.x/ScalaCache

  private def updateStatusesBasedOnNomad(statuses: Map[String, InstanceStatus]): Unit = {
    jobStatuses = statuses
    statuses.keys.foreach(requestServices)
  }

  def requestServices(id: String): Unit = {
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
        logger.debug(s"${jobRequest.uri} => ${jobServiceIds.mkString(", ")}")
        consulService.requestServiceStatus(id, jobServiceIds)
        setNomadReachable()
      case Failure(throwable) =>
        logger.error(throwable.toString)
        setNomadNotReachable()
    }
  }

  def startJob(job: JsValue): Try[Unit] = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val request = ws.url(queryUrl)
    logger.info(s"Sending job definition to ${request.uri}")
    Try {
      val result = Await.result(request.post(job), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        Success()
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status))
      }
    }.flatten
  }

  def deleteJob(id: String) = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val request = ws.url(queryUrl)
    logger.info(s"Sending deletion request to ${request.uri}")
    Try {
      val result = Await.result(request.delete(), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        jobStatuses -= id
        consulService.serviceStatuses -= id
        Success()
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status))
      }
    }.flatten
  }

}