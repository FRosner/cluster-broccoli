package de.frosner.broccoli.services

import java.net.ConnectException
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.instances.future.{getClass, _}
import cats.Traverse
import cats.instances.try_._
import cats.instances.list._

import de.frosner.broccoli.instances.{InstanceConfiguration, _}
import de.frosner.broccoli.templates.TemplateRenderer
import de.frosner.broccoli.instances.storage.InstanceStorage
import de.frosner.broccoli.logging
import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad.NomadClient
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.JsValue

import scala.concurrent
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@Singleton
@deprecated(message = "Tear apart and move functionality to the \"instances\" package", since = "2017-08-16")
class InstanceService @Inject()(nomadClient: NomadClient,
                                templateService: TemplateService,
                                nomadService: NomadService,
                                consulService: ConsulService,
                                applicationLifecycle: ApplicationLifecycle,
                                templateRenderer: TemplateRenderer,
                                instanceStorage: InstanceStorage,
                                config: Configuration) {
  private val log = play.api.Logger(getClass)

  @volatile
  var jobStatuses: Map[String, (JobStatus, Seq[PeriodicRun], Seq[String])] = Map.empty

  @volatile
  var serviceStatuses: Map[String, Seq[Service]] = Map.empty

  log.info(s"Starting $this")

  // FIXME: refactor out together with the polling scheduler
  private lazy val pollingFrequencySeconds = {
    val pollingFrequency = config.getLong("broccoli.polling.frequency").get
    if (pollingFrequency <= 0) {
      throw new IllegalArgumentException(
        s"Invalid polling frequency specified: $pollingFrequency. Needs to be a positive integer.")
    }
    pollingFrequency
  }
  log.info(s"Nomad/Consul polling frequency set to $pollingFrequencySeconds seconds")

  private val nomadScheduler = new ScheduledThreadPoolExecutor(1)
  private val nomadTask = new Runnable {
    def run() =
      logging.logExecutionTime(s"Syncing with Nomad") {
        val nomadResult = Try {
          Await
            .result(nomadService
                      .requestStatuses(instances.values.map(_.id).toSet),
                    60.seconds) // TODO make timeout configurable
        }
        nomadResult match {
          case Success(statuses) =>
            jobStatuses = statuses
            setNomadReachable()
          case Failure(throwable) =>
            jobStatuses = Map.empty
            setNomadNotReachable()
        }
      }(log.info(_))
  }
  private val scheduledNomadTask =
    nomadScheduler.scheduleWithFixedDelay(nomadTask, 0L, pollingFrequencySeconds, TimeUnit.SECONDS)

  @volatile
  private var nomadReachable: Boolean = false

  def setNomadNotReachable() = {
    nomadReachable = false
    serviceStatuses = Map.empty
  }

  def setNomadReachable() =
    nomadReachable = true

  def isNomadReachable: Boolean =
    nomadReachable

  private val consulScheduler = new ScheduledThreadPoolExecutor(1)
  private val consulTask = new Runnable {
    def run() =
      logging.logExecutionTime(s"Syncing with Consul") {
        val consulResult = Try {
          Await.result(consulService
                         .requestServicesStatuses(jobStatuses.map {
                           case (jobId, (jobStatus, periodicRuns, services)) => (jobId, services)
                         }),
                       60.seconds) // TODO make timeout configurable
        }
        consulResult match {
          case Success(result) =>
            serviceStatuses = result
            if (result.nonEmpty) // if we didn't get any results we don't really know if consul is fine
              setConsulReachable()
          case Failure(throwable) =>
            serviceStatuses = Map.empty
            setConsulNotReachable()
        }
      }(log.info(_))
  }
  private val scheduledConsulTask =
    consulScheduler.scheduleWithFixedDelay(consulTask, 0L, pollingFrequencySeconds, TimeUnit.SECONDS)

  sys.addShutdownHook {
    scheduledNomadTask.cancel(false)
    scheduledConsulTask.cancel(false)
    nomadScheduler.shutdown()
    consulScheduler.shutdown()
  }

  def getServiceStatusesOrDefault(id: String): Seq[Service] =
    serviceStatuses.getOrElse(id, Seq.empty)

  @volatile
  private var consulReachable: Boolean = false

  def setConsulNotReachable() = {
    consulReachable = false
    serviceStatuses = Map.empty
  }

  def setConsulReachable() =
    consulReachable = true

  def isConsulReachable: Boolean =
    consulReachable

  @volatile
  private var instancesMap: Map[String, Instance] = Map.empty
  @volatile
  private var instancesMapInitialized = false
  @volatile
  private def initializeInstancesMap: Map[String, Instance] = {
    instancesMapInitialized = true
    instancesMap = instanceStorage.readInstances() match {
      case Success(instances) => instances.map(instance => (instance.id, instance)).toMap
      case Failure(throwable) =>
        log.error(s"Failed to load the instances: ${throwable.toString}")
        throw throwable
    }
    instancesMap
  }

  /*
    I have to make this initialization "lazy" (but there are not lazy vars in Scala),
    because Play does not like it when you use System.exit(1) during the construction
    of dependency injected components.
   */
  @volatile
  private def instances = synchronized {
    if (instancesMapInitialized) {
      instancesMap
    } else {
      initializeInstancesMap
    }
  }

  private def getJobStatusOrDefault(id: String): JobStatus =
    if (nomadReachable) {
      jobStatuses.get(id).map { case (status, periodicStatuses, services) => status }.getOrElse(JobStatus.Stopped)
    } else {
      JobStatus.Unknown
    }

  private def getPeriodicRunsOrDefault(id: String): Seq[PeriodicRun] = {
    val periodicRuns = jobStatuses.get(id).map { case (status, periodic, services) => periodic }.getOrElse(Seq.empty)
    if (nomadReachable) {
      periodicRuns
    } else {
      periodicRuns.map(_.copy(status = JobStatus.Unknown))
    }
  }

  private def addStatuses(instance: Instance): InstanceWithStatus = {
    val instanceId = instance.id
    InstanceWithStatus(
      instance = instance,
      status = getJobStatusOrDefault(instanceId),
      services = getServiceStatusesOrDefault(instanceId),
      periodicRuns = getPeriodicRunsOrDefault(instanceId)
    )
  }

  def getInstances: Seq[InstanceWithStatus] =
    instances.values.map(addStatuses).toSeq

  def getInstance(id: String): Option[InstanceWithStatus] =
    instances.get(id).map(addStatuses)

  def addInstance(instanceCreation: InstanceCreation): Try[InstanceWithStatus] = synchronized {
    // FIXME requires ID to be defined inside the parameter values
    val templateId = instanceCreation.templateId
    val maybeId = Try(instanceCreation.parameters("id").as[String])
    val maybeCreatedInstance = maybeId
      .map { id =>
        if (instances.contains(id)) {
          Failure(new IllegalArgumentException(s"There is already an instance having the ID $id"))
        } else {
          val potentialTemplate = templateService.template(templateId)
          potentialTemplate
            .map { template =>
              Try {
                instanceCreation.parameters.map { param =>
                  ParameterValue.constructParameterValueFromJson(param._1, template, param._2) match {
                    case Success(s)  => (param._1, s)
                    case Failure(ex) => throw ex
                  }
                }
              } match {
                case Success(params) =>
                  // Remove params with default values
                  val parameters = params.filter { param =>
                    !param._2.isEmpty
                  }
                  val maybeNewInstance = Try(Instance(id, template, parameters))
                  maybeNewInstance.flatMap { newInstance =>
                    val result = instanceStorage.writeInstance(newInstance)
                    result.foreach { writtenInstance =>
                      instancesMap = instancesMap.updated(id, writtenInstance)
                    }
                    result
                  }
                case Failure(ex) =>
                  Failure(ex)
              }
            }
            .getOrElse(Failure(new IllegalArgumentException(s"Template $templateId does not exist.")))
        }
      }
      .getOrElse(Failure(new IllegalArgumentException("No ID specified")))
    maybeCreatedInstance.map(addStatuses)
  }

  def updateInstance(id: String,
                     statusUpdater: Option[JobStatus],
                     parameterValuesUpdater: Option[Map[String, JsValue]],
                     templateSelector: Option[String],
                     periodicJobsToStop: Option[Seq[String]]): Try[InstanceWithStatus] = synchronized {
    val updateInstanceId = id
    val maybeInstance = instances.get(id)
    val maybeUpdatedInstance = maybeInstance.map { instance =>
      Try {
        parameterValuesUpdater.map { p =>
          p.map { e =>
            val parameterName = e._1
            val jsValue = e._2
            ParameterValue.constructParameterValueFromJson(parameterName, instance.template, jsValue) match {
              case Success(pValue) => (parameterName, pValue)
              case Failure(ex)     => throw ex
            }
          }
        }
      } match {
        case Success(parsedParamValuesUpdated) =>
          val parameterValuesUpdatesWithPossibleDefaults = parsedParamValuesUpdated.map { p =>
            p.filter {
              case (_, value) => !value.isEmpty
            }
          }

          val instanceWithPotentiallyUpdatedTemplateAndParameterValues: Try[Instance] =
            if (templateSelector.isDefined) {
              val newTemplateId = templateSelector.get
              val newTemplate = templateService.template(newTemplateId)
              newTemplate
                .map { template =>
                  // Requested template exists, update the template
                  if (parameterValuesUpdatesWithPossibleDefaults.isDefined) {
                    // New parameter values are specified
                    val newParameterValues = parameterValuesUpdatesWithPossibleDefaults.get
                    instance.updateTemplate(template, newParameterValues)
                  } else {
                    // Just use the old parameter values
                    instance.updateTemplate(template, instance.parameterValues)
                  }
                }
                .getOrElse {
                  // New template does not exist
                  Failure(TemplateNotFoundException(newTemplateId))
                }
            } else {
              // No template update required
              if (parameterValuesUpdatesWithPossibleDefaults.isDefined) {
                // Just update the parameter values
                val newParameterValues = parameterValuesUpdatesWithPossibleDefaults.get
                instance.updateParameterValues(newParameterValues)
              } else {
                // Neither template update nor parameter value update required
                Success(instance)
              }
            }

          val instanceWithPotentiallyStoppedPeriodicRuns =
            instanceWithPotentiallyUpdatedTemplateAndParameterValues.flatMap { instance =>
              val instancePeriodicRunNames = addStatuses(instance).periodicRuns.map(_.jobName)
              periodicJobsToStop match {
                case Some(toStop) =>
                  val nonExistingPeriodicJobs = toStop.toSet -- instancePeriodicRunNames.toSet
                  nonExistingPeriodicJobs.headOption match {
                    case None =>
                      // Periodic jobs are all valid and belong to this instance
                      val tryToDelete = Traverse[List].sequence(toStop.map(nomadService.deleteJob).toList)
                      tryToDelete.map(_ => instance)
                    case Some(job) =>
                      // There is at least one periodic job that doesn't belong to this instance
                      Failure(PeriodicJobNotFoundException(instance.id, job))
                  }
                case None => Success(instance)
              }
            }

          val updatedInstance = instanceWithPotentiallyStoppedPeriodicRuns.flatMap { instance =>
            statusUpdater
              .map {
                // Update the instance status
                case JobStatus.Running =>
                  nomadService.startJob(templateRenderer.renderJson(instance)).map(_ => instance)
                case JobStatus.Stopped =>
                  val deletedJob = nomadService.deleteJob(instance.id)
                  // FIXME we don't delete the service and periodic job / status info here (#352) => potential mem leak
                  deletedJob
                    .map(_ => instance)
                case other =>
                  Failure(new IllegalArgumentException(s"Unsupported status change received: $other"))
              }
              .getOrElse {
                // Don't update the instance status
                Success(instance)
              }
          }

          updatedInstance match {
            case Failure(throwable)       => log.error(s"Error updating instance: $throwable")
            case Success(changedInstance) => log.debug(s"Successfully applied an update to $changedInstance")
          }

          updatedInstance
            .flatMap { instance =>
              val maybeWrittenInstance = instanceStorage.writeInstance(instance)
              maybeWrittenInstance.foreach(written => instancesMap = instancesMap.updated(id, written))
              maybeWrittenInstance
            }
            .map(addStatuses)
        case Failure(parameterValidationError) => Failure(parameterValidationError)
      }
    }
    maybeUpdatedInstance match {
      case Some(tryUpdatedInstance) => tryUpdatedInstance
      case None                     => Failure(InstanceNotFoundException(id))
    }
  }

  def deleteInstance(id: String): Try[InstanceWithStatus] = synchronized {
    val tryStopping = updateInstance(
      id = id,
      statusUpdater = Some(JobStatus.Stopped),
      parameterValuesUpdater = None,
      templateSelector = None,
      periodicJobsToStop = None
    )
    val tryDelete = tryStopping
      .map(_.instance)
      .recover {
        case throwable: ConnectException => {
          // TODO #144
          log.warn(s"Failed to stop $id. It might be running when Nomad is reachable again.")
          instancesMap(id)
        }
      }
      .flatMap { stoppedInstance =>
        val tryDelete = instanceStorage.deleteInstance(stoppedInstance)
        tryDelete.foreach(instance => instancesMap -= instance.id)
        tryDelete
      }
    tryDelete.map(addStatuses)
  }

}
