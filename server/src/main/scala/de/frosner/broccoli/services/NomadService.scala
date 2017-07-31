package de.frosner.broccoli.services

import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named, Singleton}
import javax.xml.ws.http.HTTPException

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.JobStatus._
import de.frosner.broccoli.models.{Instance, JobStatus, PeriodicRun}
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.nomad.models.Job
import de.frosner.broccoli.util.Logging
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.Configuration

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@Singleton
class NomadService @Inject()(nomadConfiguration: NomadConfiguration, consulService: ConsulService, ws: WSClient)
    extends Logging {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = nomadConfiguration.url

  @volatile
  var jobStatuses: Map[String, (JobStatus, Seq[PeriodicRun])] = Map.empty

  def getJobStatusOrDefault(id: String): JobStatus =
    if (nomadReachable) {
      jobStatuses.get(id).map { case (status, periodicStatuses) => status }.getOrElse(JobStatus.Stopped)
    } else {
      JobStatus.Unknown
    }

  def getPeriodicRunsOrDefault(id: String): Seq[PeriodicRun] = {
    val periodicRuns = jobStatuses.get(id).map { case (status, periodic) => periodic }.getOrElse(Seq.empty)
    if (nomadReachable) {
      periodicRuns
    } else {
      periodicRuns.map(_.copy(status = JobStatus.Unknown))
    }
  }

  @volatile
  private var nomadReachable: Boolean = false

  def setNomadNotReachable() = {
    nomadReachable = false
    consulService.serviceStatuses = Map.empty
  }

  def setNomadReachable() =
    nomadReachable = true

  def isNomadReachable: Boolean =
    nomadReachable

  def requestStatuses(instanceIds: Set[String]) = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val jobsRequest = ws.url(queryUrl)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, statuses) =
        ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Status").map(_.as[JsString].value))
      (ids, statuses)
    })
    jobsWithTemplate.onComplete {
      case Success((ids, statuses)) => {
        Logger.debug(s"${jobsRequest.uri} => ${ids.zip(statuses).mkString(", ")}")
        val idsAndStatuses = ids.zip(statuses.map {
          case "running" => JobStatus.Running
          case "pending" => JobStatus.Pending
          case "dead"    => JobStatus.Dead
          case default =>
            Logger.warn(s"Unmatched status received: $default")
            JobStatus.Unknown
        })
        val idsAndStatusesNotOnNomad = (instanceIds -- ids.toSet).map((_, JobStatus.Stopped))
        val filteredIdsAndStatuses = idsAndStatuses.filter {
          case (id, status) => instanceIds.contains(id)
        }
        val filteredIdsAndStatusesAlsoWithoutNomad = idsAndStatusesNotOnNomad ++ idsAndStatuses
        val periodicStatuses = filteredIdsAndStatusesAlsoWithoutNomad
          .flatMap {
            case (id, status) =>
              instanceIds
                .find(instanceId => id.startsWith(s"$instanceId/periodic-"))
                .map(instanceId => (instanceId, (id, status)))
          }
          .foldLeft(Map.empty[String, Map[String, JobStatus]]) {
            case (mappedStatuses, (instanceId, (periodicJobId, periodicJobStatus))) =>
              val instanceStatuses = mappedStatuses.get(instanceId)
              val newInstanceStatuses = instanceStatuses
                .map(_.updated(periodicJobId, periodicJobStatus))
                .getOrElse(Map(periodicJobId -> periodicJobStatus))
              mappedStatuses.updated(instanceId, newInstanceStatuses)
          }
          .map {
            case (instanceId, periodicRunStatuses) =>
              val periodic = periodicRunStatuses.map {
                case (periodicJobName, periodicJobStatus) =>
                  PeriodicRun(
                    createdBy = instanceId,
                    status = periodicJobStatus,
                    utcSeconds = NomadService.extractUtcSeconds(periodicJobName).getOrElse(0),
                    jobName = periodicJobName
                  )
              }.toSeq
              (instanceId, periodic)
          }
        val idsAndStatusesWithPeriodic = filteredIdsAndStatusesAlsoWithoutNomad.map {
          case (instanceId, instanceStatus) =>
            (instanceId, (instanceStatus, periodicStatuses.getOrElse(instanceId, Seq.empty)))
        }
        setNomadReachable()
        jobStatuses = idsAndStatusesWithPeriodic.toMap
        filteredIdsAndStatuses.foreach(p => requestServices(p._1))
      }
      case Failure(throwable: ConnectException) => {
        Logger.error(
          s"Nomad did not respond when requesting services for ${instanceIds.mkString(", ")} from ${jobsRequest.uri}: $throwable")
        setNomadNotReachable()
      }
      case Failure(throwable) => {
        Logger.warn(s"Failed to request statuses for ${instanceIds.mkString(", ")} from ${jobsRequest.uri}: $throwable")
      }
    }
  }

  // TODO should this go back to InstanceService or how? I mean I need to check the existing instances in order to know whether
  // TODO something is stopped (i.e. not present in Nomad). Or we use a cache that let's the stuff expire if you don't get an answer from Nomad
  // https://www.playframework.com/documentation/2.5.x/ScalaCache

  def requestServices(id: String): Future[Seq[String]] = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val jobRequest = ws.url(queryUrl)
    val jobResponse = jobRequest.get().map { response =>
      if (response.status == 200) {
        response.json.as[Job]
      } else {
        throw new Exception(s"Received ${response.statusText} (${response.status})")
      }
    }
    val eventuallyJobServiceIds = jobResponse.map { job =>
      val taskGroups = job.taskGroups.getOrElse(Seq.empty)
      val tasks = taskGroups.flatMap(_.tasks.getOrElse(Seq.empty))
      val services = tasks.flatMap(_.services.getOrElse(Seq.empty))
      services.map(_.name)
    }
    val eventuallyRequestedServices = eventuallyJobServiceIds.map { jobServiceIds =>
      Logger.debug(s"${jobRequest.uri} => ${jobServiceIds.mkString(", ")}")
      consulService.requestServiceStatus(id, jobServiceIds)
      jobServiceIds
    }
    eventuallyRequestedServices.onFailure {
      case throwable =>
        Logger.error(s"Requesting services for $id failed: $throwable")
    }
    eventuallyRequestedServices
  }

  def startJob(job: JsValue): Try[Unit] = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending job definition to ${request.uri}")
    Try {
      val result = Await.result(request.post(job), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        Success(())
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status))
      }
    }.flatten
  }

  def deleteJob(id: String) = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val request = ws.url(queryUrl)
    Logger.info(s"Sending deletion request to ${request.uri}")
    Try {
      val result = Await.result(request.delete(), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        // TODO doesn't work with periodic runs so we have to remove it, in order to avoid a memory leak we should use a cache with TTLs
        // jobStatuses -= id
        consulService.serviceStatuses -= id
        Success(())
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status))
      }
    }.flatten
  }

}

object NomadService {

  def extractUtcSeconds(periodicJobName: String): Try[Long] = Try {
    val Array(jobName, periodicSuffix) = periodicJobName.split("/")
    periodicSuffix.stripPrefix("periodic-").toLong
  }

}
