package de.frosner.broccoli.services

import java.net.ConnectException
import java.util.concurrent.TimeUnit

import de.frosner.broccoli.auth.Account
import javax.inject.{Inject, Singleton}
import de.frosner.broccoli.models.JobStatus._
import de.frosner.broccoli.models.{JobStatus, PeriodicRun}
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.nomad.models._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class NomadService @Inject()(nomadConfiguration: NomadConfiguration, ws: WSClient)(implicit ec: ExecutionContext) {

  private val nomadBaseUrl = nomadConfiguration.url

  private val log = play.api.Logger(getClass)

  private lazy val namespaces: Future[Set[String]] =
    if (nomadConfiguration.namespacesEnabled) {
      val queryUrl = nomadBaseUrl + "/v1/namespaces"
      requestWithHeaders(queryUrl)
        .get()
        .map(_.json.as[JsArray])
        .map(jsArray => {
          jsArray \\ "Name" map (_.as[JsString].value)
        })
        .map(_.toSet)
    } else Future.successful(Set.empty[String])

  val headers = sys.env.get(nomadConfiguration.tokenEnvName).map(authToken => ("X-Nomad-Token", authToken)).toList

  case class JobInfoInternal(jobId: String, namespace: Option[String], status: JobStatus.JobStatus)

  log.info(s"Starting $this")

  def requestWithHeaders(url: String): WSRequest =
    ws.url(url).withHeaders(headers: _*)

  private def getJobsAndStatus(queryUrl: String, namespace: Option[String]): Future[List[JobInfoInternal]] =
    requestWithNamespace(requestWithHeaders(queryUrl), namespace)
      .get()
      .map(_.json.as[List[JsValue]])
      .map(jsArray => {
        jsArray.map(jsObject => {
          val jobId = (jsObject \ "ID").as[String]
          val jobStatus = (jsObject \ "Status").as[String] match {
            case "running" => JobStatus.Running
            case "pending" => JobStatus.Pending
            case "dead"    => JobStatus.Dead
            case default =>
              log.warn(s"Unmatched status received: $default")
              JobStatus.Unknown
          }
          JobInfoInternal(jobId, namespace, jobStatus)
        })
      })

  private def requestWithNamespace(request: WSRequest, maybeNamespace: Option[String]): WSRequest =
    maybeNamespace.map(namespace => request.withQueryString(("namespace", namespace))).getOrElse(request)

  def requestStatuses(instanceIds: Set[String]): Future[Map[String, (JobStatus, Seq[PeriodicRun], Seq[String])]] = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val jobsWithTemplate = namespaces
      .map(namespaceSet => {
        if (namespaceSet.nonEmpty) {
          Future
            .sequence(namespaceSet.map(namespace => {
              getJobsAndStatus(queryUrl, Some(namespace))
            }))
            .map(_.flatten)
        } else {
          getJobsAndStatus(queryUrl, None)
        }
      })
      .flatMap(identity)
    val idsAndStatusesWithPeriodicF = jobsWithTemplate.map { idsAndStatuses =>
      {
        log.debug(s"$queryUrl => ${idsAndStatuses.mkString(", ")}")
        val ids = idsAndStatuses.map(_.jobId)
        val idsAndStatusesNotOnNomad = (instanceIds -- ids).map(JobInfoInternal(_, None, JobStatus.Stopped))
        val filteredIdsAndStatuses = idsAndStatuses.filter(jobInfo => instanceIds.contains(jobInfo.jobId))
        val filteredIdsAndStatusesAlsoWithoutNomad = idsAndStatusesNotOnNomad ++ idsAndStatuses
        val periodicStatuses = filteredIdsAndStatusesAlsoWithoutNomad
          .flatMap { jobInfo =>
            instanceIds
              .find(instanceId => jobInfo.jobId.startsWith(s"$instanceId/periodic-"))
              .map(instanceId => (instanceId, jobInfo))
          }
          .foldLeft(Map.empty[String, Set[JobInfoInternal]]) {
            case (mappedStatuses, (instanceId, periodicJobInfo)) =>
              val instanceStatuses = mappedStatuses.get(instanceId)
              val newInstanceStatuses = instanceStatuses
                .map(_.+(periodicJobInfo))
                .getOrElse(Set(periodicJobInfo))
              mappedStatuses.updated(instanceId, newInstanceStatuses)
          }
          .map {
            case (instanceId, periodicJobStatuses) =>
              val periodic = periodicJobStatuses.map {
                case JobInfoInternal(periodicJobName, _, periodicJobStatus) =>
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
          case JobInfoInternal(instanceId, _, instanceStatus) =>
            (instanceId, (instanceStatus, periodicStatuses.getOrElse(instanceId, Seq.empty)))
        }
        (filteredIdsAndStatuses, idsAndStatusesWithPeriodic.toMap)
      }
    }
    val serviceRequestsDone = idsAndStatusesWithPeriodicF.flatMap {
      case (jobIdsThatExistOnNomad, idsAndStatusesWithPeriodic) =>
        val servicesF = jobIdsThatExistOnNomad.map { job =>
          requestServices(job.jobId, job.namespace).map(services => (job.jobId, services))
        }
        Future.sequence(servicesF).map { services =>
          val servicesMap = services.toMap
          idsAndStatusesWithPeriodic.map {
            case (jobId, (jobStatus, periodicRuns)) =>
              (jobId, (jobStatus, periodicRuns, servicesMap.getOrElse(jobId, Seq.empty)))
          }
        }
    }
    serviceRequestsDone.onFailure {
      case throwable: ConnectException => {
        log.error(
          s"Nomad did not respond when requesting services for ${instanceIds.mkString(", ")} from ${queryUrl}: $throwable")
      }
      case throwable => {
        log.warn(s"Failed to request statuses for ${instanceIds.mkString(", ")} from ${queryUrl}: $throwable")
      }
    }
    serviceRequestsDone
  }

  // TODO should this go back to InstanceService or how? I mean I need to check the existing instances in order to know whether
  // TODO something is stopped (i.e. not present in Nomad). Or we use a cache that let's the stuff expire if you don't get an answer from Nomad
  // https://www.playframework.com/documentation/2.5.x/ScalaCache

  def requestServices(id: String, namespace: Option[String] = None): Future[Seq[String]] = {
    val services = for {
      response <- requestWithNamespace(requestWithHeaders(nomadBaseUrl + s"/v1/job/$id"), namespace).get()
      job = if (response.status == 200) {
        response.json.as[Job]
      } else {
        throw new Exception(s"Received ${response.statusText} (${response.status})")
      }
      services = for {
        group <- job.taskGroups
        task <- group.tasks
        service <- task.services.getOrElse(Seq.empty)
      } yield service.name
    } yield {
      log.debug(s"${requestWithHeaders(nomadBaseUrl + s"/v1/job/$id").uri} => ${services.mkString(", ")}")
      services
    }
    services.onFailure {
      case throwable =>
        log.error(s"Requesting services for $id failed: ${throwable.getMessage}", throwable)
    }
    services
  }

  def startJob(job: JsValue): Try[Unit] = {
    val queryUrl = nomadBaseUrl + "/v1/jobs"
    val request = requestWithHeaders(queryUrl)
    log.info(s"Sending job definition to ${request.uri}")
    log.debug(s"JobDefinition: ${job.toString()}")
    Try {
      val result = Await.result(request.post(job), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        Success(())
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status, result.body))
      }
    }.flatten
  }

  def deleteJob(id: String, namespace: Option[String]): Try[String] = {
    val queryUrl = nomadBaseUrl + s"/v1/job/$id"
    val request = requestWithNamespace(requestWithHeaders(queryUrl), namespace)
    log.info(s"Sending deletion request to ${request.uri}")
    Try {
      val result = Await.result(request.delete(), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        // TODO doesn't work with periodic runs so we have to remove it, in order to avoid a memory leak we should use a cache with TTLs
        // jobStatuses -= id
        Success(id)
      } else {
        Failure(NomadRequestFailed(request.uri.toString, result.status))
      }
    }.flatten
  }

  /**
    * Takes in a HCL job and converts it to JSON to be posted to the web server
    */
  def parseHCLJob(hclJob: String): Try[JsValue] = {
    val endPoint = nomadBaseUrl + "/v1/jobs/parse"
    val request = requestWithHeaders(endPoint)
    Try {
      val requestJson = JsObject(Seq("JobHCL" -> JsString(hclJob), "Canonicalize" -> JsBoolean(true)))
      val result =
        Await.result(request.post(requestJson), Duration(5, TimeUnit.SECONDS))
      if (result.status == 200) {
        log.info("Successfully converted HCL job to JSON")
        Success(result.json)
      } else {
        log.error(s"Error parsing job HCL. Request body: ${requestJson.toString()}")
        Failure(NomadRequestFailed(request.uri.toString, result.status, result.body))
      }
    }.flatten
  }

  def getNodeResources(loggedIn: Account): Future[Seq[NodeResources]] = {
    // Step 1: Get all nodes
    val queryUrl = nomadBaseUrl + s"/v1/nodes"
    log.info("Contacting: " + nomadBaseUrl)
    val scheme = Option(new java.net.URI(nomadBaseUrl).getScheme).getOrElse("http")
    val request = requestWithHeaders(queryUrl)
    log.info(s"Sending get nodes request to ${request.uri}")
    val oePrefix = loggedIn.getOEPrefix
    request
      .get()
      .flatMap(queryResponse => {
        val filteredIds =
          for {
            jsValue <- queryResponse.json.as[List[JsValue]]
            id = (jsValue \ "ID").as[String]
            name = (jsValue \ "Name").as[String]
            // Step 2: Filter out node ids that start with the oe-prefix
            if name.split("-")(0).equals(oePrefix)
          } yield (id, name)
        Future
          .sequence(
            filteredIds.map {
              case (id, name) => {
                // Step 3: Query a particular node for the http address to its nomad client and the Total Resources
                val ipUrl = nomadBaseUrl + s"/v1/node/$id"
                requestWithHeaders(ipUrl)
                  .get()
                  .map(
                    ipResponse =>
                      (
                        (ipResponse.json \ "HTTPAddr").as[String],
                        (ipResponse.json \ "Attributes" \ "unique.storage.volume").as[String],
                        // this logic is copied from
                        // github.com/hashicorp/nomad/blob/22fd62753510a4a41c1b8f1d117ea1a90b48df06/command/node_status.go#L631
                        TotalResources(
                          (ipResponse.json \ "Resources" \ "CPU").as[Long] -
                            (ipResponse.json \ "Reserved" \ "CPU").as[Long],
                          (ipResponse.json \ "Resources" \ "MemoryMB").as[Long] -
                            (ipResponse.json \ "Reserved" \ "MemoryMB").as[Long],
                          (ipResponse.json \ "Resources" \ "DiskMB").as[Long] -
                            (ipResponse.json \ "Reserved" \ "DiskMB").as[Long]
                        )
                    )
                  )
                  .map {
                    case (httpAddr, storageDevice, totalResources) => {
                      // Step 4: Query the nomad client on a node for resource stats (Host Resources)
                      val hostStatsUrl = s"$scheme://$httpAddr/v1/client/stats"
                      val hostResult =
                        requestWithHeaders(hostStatsUrl)
                          .get()
                          .map(
                            resourceResponse => {
                              val cpu = Math.round(Math.floor((resourceResponse.json \ "CPUTicksConsumed").as[Double]))
                              val memoryUsed = (resourceResponse.json \ "Memory" \ "Used").as[Long]
                              val memoryTotal = (resourceResponse.json \ "Memory" \ "Total").as[Long]
                              val (diskUsed, diskSize) =
                                (resourceResponse.json \ "DiskStats")
                                  .as[List[JsValue]]
                                  .filter(diskJson => (diskJson \ "Device").as[String].equals(storageDevice))
                                  .map(diskJson => ((diskJson \ "Used").as[Long], (diskJson \ "Size").as[Long])) match {
                                  case (dUsed, dSize) :: _ => (dUsed, dSize)
                                  case _                   => (0L, 0L) // This should never happen normally
                                }
                              HostResources(cpu, memoryUsed, memoryTotal, diskUsed, diskSize)
                            }
                          )
                      // Allocations
                      // Step 5: Get all allocations for a particular node and the resources requested by it
                      val allocationUrl = nomadBaseUrl + s"/v1/node/$id/allocations"
                      val allocationResults =
                        requestWithHeaders(allocationUrl)
                          .get()
                          .map(
                            allocationsResponse => {
                              val allocationsJson = allocationsResponse.json.as[List[JsValue]]
                              allocationsJson
                                .filter(allocationJson => (allocationJson \ "ClientStatus").as[String] == "running")
                                .map(
                                  allocationJson =>
                                    AllocatedResources(
                                      (allocationJson \ "ID").as[String],
                                      (allocationJson \ "Name").as[String],
                                      (allocationJson \ "Namespace").asOpt[String],
                                      (allocationJson \ "Resources" \ "CPU").as[Long],
                                      (allocationJson \ "Resources" \ "MemoryMB").as[Long],
                                      (allocationJson \ "Resources" \ "DiskMB").as[Long]
                                  )
                                )
                            }
                          )
                      // Allocation Utilizations
                      val allocationUtilizationResult =
                        allocationResults
                          .map(allocations => {
                            Future.sequence(
                              allocations.map(
                                allocatedResources => {
                                  // Step 5: Get the allocation stats for each allocation id from the nomad client
                                  //          running on a node
                                  val allocationUtilizationUrl =
                                    s"$scheme://$httpAddr/v1/client/allocation/${allocatedResources.id}/stats"
                                  requestWithNamespace(requestWithHeaders(allocationUtilizationUrl),
                                                       allocatedResources.namespace)
                                    .get()
                                    .map(
                                      allocationStatsResponse => {
                                        AllocatedResourcesUtilization(
                                          allocatedResources.id,
                                          allocatedResources.name,
                                          Math.round(
                                            Math.floor(
                                              (allocationStatsResponse.json \ "ResourceUsage" \ "CpuStats" \ "TotalTicks")
                                                .as[Double]
                                            )
                                          ),
                                          (allocationStatsResponse.json \ "ResourceUsage" \ "MemoryStats" \ "RSS")
                                            .as[Long]
                                        )
                                      }
                                    )
                                }
                              )
                            )
                          })
                          .flatMap(identity)
                      // Step 6: Combine all results into a Future[NodeResources]
                      for {
                        hostResources <- hostResult
                        allocations <- allocationResults
                        allocationsUtilization <- allocationUtilizationResult
                      } yield {
                        val (allocCPU, allocMem, allocDisk) =
                          allocations.foldLeft((0L, 0L, 0L)) {
                            case ((cpu, memMB, diskMB), allocation) =>
                              (cpu + allocation.cpu, memMB + allocation.memoryMB, diskMB + allocation.diskMB)
                          }
                        val (allocUtilCPU, allocUtilMem) =
                          allocationsUtilization.foldLeft((0L, 0L)) {
                            case ((cpu, mem), allocationUtlization) =>
                              (cpu + allocationUtlization.cpu, mem + allocationUtlization.memory)
                          }
                        NodeResources(
                          id,
                          name,
                          totalResources,
                          hostResources,
                          Map(allocations.map(allocation => (allocation.id, allocation)): _*),
                          Map(allocationsUtilization
                            .map(allocationUtilization => (allocationUtilization.id, allocationUtilization)): _*),
                          TotalResources(allocCPU, allocMem, allocDisk),
                          TotalUtilization(allocUtilCPU, allocUtilMem)
                        )
                      }
                    }
                  }
                  .flatMap(identity)
                  .map(Success(_))
                  .recover({ case ex => Failure(ex) })
              }
            }
          )
          .map(
            _.flatMap(
              tryResult =>
                tryResult match {
                  case Success(resourceStats) => List(resourceStats)
                  case Failure(ex) =>
                    log.error("Error while getting node information: {}", ex)
                    List()
              }
            )
          )
      })
  }

}

object NomadService {

  def extractUtcSeconds(periodicJobName: String): Try[Long] = Try {
    val Array(jobName, periodicSuffix) = periodicJobName.split("/")
    periodicSuffix.stripPrefix("periodic-").toLong
  }

}
