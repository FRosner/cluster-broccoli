package de.frosner.broccoli.services

import java.io.{ObjectOutputStream, _}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named, Singleton}

import akka.actor._
import de.frosner.broccoli.models._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.services.NomadService.{DeleteJob, GetServices, GetStatuses, StartJob}
import de.frosner.broccoli.util.Logging
import play.api.{Configuration, Play}
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import InstanceService._
import de.frosner.broccoli.conf
import play.Logger
import play.api.inject.ApplicationLifecycle

import scala.io.Source

@Singleton
class InstanceService @Inject()(templateService: TemplateService,
                                system: ActorSystem,
                                @Named("nomad-actor") nomadActor: ActorRef,
                                ws: WSClient,
                                configuration: Configuration,
                                lifecycle: ApplicationLifecycle) extends Actor with Logging {

  private val pollingFrequencySecondsString = configuration.getString(conf.POLLING_FREQUENCY_KEY)
  private val pollingFrequencySecondsTry = pollingFrequencySecondsString match {
    case Some(string) => Try(string.toInt).flatMap {
      int => if (int >= 1) Success(int) else Failure(new Exception())
    }
    case None => Success(conf.POLLING_FREQUENCY_DEFAULT)
  }
  if (pollingFrequencySecondsTry.isFailure) {
    Logger.error(s"Invalid ${conf.POLLING_FREQUENCY_KEY} specified: '${pollingFrequencySecondsString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  private val pollingFrequencySeconds = pollingFrequencySecondsTry.get
  Logger.info(s"Nomad/Consul polling frequency set to $pollingFrequencySeconds seconds")
  private val cancellable: Cancellable = system.scheduler.schedule(
    initialDelay = Duration.Zero,
    interval = Duration.create(pollingFrequencySeconds, TimeUnit.SECONDS),
    receiver = nomadActor,
    message = GetStatuses
  )(
    system.dispatcher,
    self
  )

  val nomadJobPrefix = {
    val prefix = conf.getNomadJobPrefix(configuration)
    Logger.info(s"${conf.NOMAD_JOB_PREFIX_KEY}=$prefix")
    prefix
  }

  private val instanceStorage: InstanceStorage = {
    val instanceStorageType = {
      val storageType = configuration.getString(conf.INSTANCES_STORAGE_TYPE_KEY).getOrElse(conf.TEMPLATES_STORAGE_TYPE_DEFAULT)
      val allowedStorageTypes = Set(conf.INSTANCES_STORAGE_TYPE_FS, conf.INSTANCES_STORAGE_TYPE_COUCHDB)
      if (!allowedStorageTypes.contains(storageType)) {
        Logger.error(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType is invalid. Only '${allowedStorageTypes.mkString(", ")} supported.")
        System.exit(1)
      }
      Logger.info(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType")
      storageType
    }

    val instanceStorageUrl = {
      if (configuration.getString("broccoli.instancesFile").isDefined) Logger.warn(s"broccoli.instancesDir ignored. Use ${conf.INSTANCES_STORAGE_URL_KEY} instead.")
      val url = configuration.getString(conf.INSTANCES_STORAGE_URL_KEY).getOrElse(conf.INSTANCES_STORAGE_URL_DEFAULT_FS)
      Logger.info(s"${conf.INSTANCES_STORAGE_URL_KEY}=$url")
      url
    }

    instanceStorageType match {
      case conf.INSTANCES_STORAGE_TYPE_FS => {
        val maybeStorage = Try(FileSystemInstanceStorage(new File(instanceStorageUrl), nomadJobPrefix))
        maybeStorage match {
          case Success(storage) => storage
          case Failure(throwable) =>
            Logger.error(s"Cannot start file system instance storage: ${throwable.toString()})")
            System.exit(1)
            throw throwable
        }
      }
      case conf.INSTANCES_STORAGE_TYPE_COUCHDB => ???
      case default => throw new IllegalStateException(s"Illegal storage type '${instanceStorageType}")
    }
  }

  sys.addShutdownHook {
    instanceStorage.close()
  }

  // TODO these guys need to populate the instances to InstanceWithStatus before sending them to the front-end
  // TODO I need to clean this stuff up (when deleting an instance)
  @volatile
  private var jobStatuses: Map[String, InstanceStatus] = Map.empty

  @volatile
  private var serviceStatuses: Map[String, Map[String, Service]] = Map.empty

  private def getJobStatusOrDefault(id: String): InstanceStatus = jobStatuses.getOrElse(id, InstanceStatus.Unknown)

  private def getServiceStatusesOrDefault(id: String): Map[String, Service] =
    serviceStatuses.getOrElse(id, Map.empty)

  private def addStatuses(instance: Instance): InstanceWithStatus = {
    val instanceId = instance.id
    InstanceWithStatus(
      instance = instance,
      status = getJobStatusOrDefault(instanceId),
      services = getServiceStatusesOrDefault(instanceId)
    )
  }

  implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def receive = {
    case GetInstances => sender ! getInstances
    case GetInstance(id) => sender ! getInstance(id)
    case NewInstance(instanceCreation) => sender() ! addInstance(instanceCreation)
    case updateInstanceInfo: UpdateInstance => sender() ! updateInstance(updateInstanceInfo)
    case DeleteInstance(id) => sender ! deleteInstance(id)
    case NomadStatuses(statuses) => updateStatusesBasedOnNomad(statuses)
    case ConsulServices(id, services) => updateServicesBasedOnNomad(id, services)
    case NomadNotReachable => setAllStatusesToUnknown()
  }

  private[this] def setAllStatusesToUnknown(): Unit = {
    jobStatuses = jobStatuses.map {
      case (key, value) => (key, InstanceStatus.Unknown)
    }
  }

  private[this] def updateStatusesBasedOnNomad(statuses: Map[String, InstanceStatus]): Unit = {
    val updatedInstances = instanceStorage.readInstances.map { instances =>
      instances.foreach { instance =>
        val id = instance.id
        statuses.get(id) match {
          case Some(nomadStatus) =>
            jobStatuses = jobStatuses.updated(id, nomadStatus)
            nomadActor ! GetServices(id)
          case None =>
            jobStatuses = jobStatuses.updated(id, InstanceStatus.Stopped)
            serviceStatuses = serviceStatuses.updated(id, Map.empty)
        }
      }
    }
  }

  private[this] def updateServicesBasedOnNomad(jobId: String, services: Iterable[Service]) = {
    serviceStatuses = serviceStatuses.updated(jobId, services.map(service => (service.name, service)).toMap)
  }

  private[this] def getInstances: Try[Iterable[InstanceWithStatus]] = {
    instanceStorage.readInstances.map(_.map(addStatuses))
  }

  private[this] def getInstance(id: String): Try[InstanceWithStatus] = {
    instanceStorage.readInstance(id).map(addStatuses)
  }

  private[this] def addInstance(instanceCreation: InstanceCreation): Try[InstanceWithStatus] = {
    val maybeId = instanceCreation.parameters.get("id") // FIXME requires ID to be defined inside the parameter values
    val templateId = instanceCreation.templateId
    val maybeCreatedInstance = maybeId.map { id =>
      if (instanceStorage.readInstance(id).isSuccess) {
        Failure(newExceptionWithWarning(new IllegalArgumentException(s"There is already an instance having the ID $id")))
      } else {
        val potentialTemplate = templateService.template(templateId)
        potentialTemplate.map { template =>
          val maybeNewInstance = Try(Instance(id, template, instanceCreation.parameters))
          maybeNewInstance.flatMap(instanceStorage.writeInstance)
        }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException(s"Template $templateId does not exist."))))
      }
    }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException("No ID specified"))))
    maybeCreatedInstance.map(addStatuses)
  }

  private[this] def updateInstance(updateInstance: UpdateInstance): Try[InstanceWithStatus] = {
    val updateInstanceId = updateInstance.id
    val maybeInstance = instanceStorage.readInstance(updateInstanceId)
    maybeInstance.flatMap { instance =>
      val instanceWithPotentiallyUpdatedTemplateAndParameterValues: Try[Instance] = if (updateInstance.templateSelector.isDefined) {
        val newTemplateId = updateInstance.templateSelector.get.newTemplateId
        val newTemplate = templateService.template(newTemplateId)
        newTemplate.map { template =>
          // Requested template exists, update the template
          if (updateInstance.parameterValuesUpdater.isDefined) {
            // New parameter values are specified
            val newParameterValues = updateInstance.parameterValuesUpdater.get.newParameterValues
            instance.updateTemplate(template, newParameterValues)
          } else {
            // Just use the old parameter values
            instance.updateTemplate(template, instance.parameterValues)
          }
        }.getOrElse {
          // New template does not exist
          Failure(TemplateNotFoundException(newTemplateId))
        }
      } else {
        // No template update required
        if (updateInstance.parameterValuesUpdater.isDefined) {
          // Just update the parameter values
          val newParameterValues = updateInstance.parameterValuesUpdater.get.newParameterValues
          instance.updateParameterValues(newParameterValues)
        } else {
          // Neither template update nor parameter value update required
          Success(instance)
        }
      }
      val updatedInstance = instanceWithPotentiallyUpdatedTemplateAndParameterValues.map { instance =>
        updateInstance.statusUpdater.map {
          // Update the instance status
          case StatusUpdater(InstanceStatus.Running) =>
            nomadActor.tell(StartJob(instance.templateJson), self)
            Success(instance)
          case StatusUpdater(InstanceStatus.Stopped) =>
            nomadActor.tell(DeleteJob(instance.id), self)
            Success(instance)
          case other =>
            Failure(new IllegalArgumentException(s"Unsupported status change received: $other"))
        }.getOrElse {
          // Don't update the instance status
          Success(instance)
        }
      }.flatten


      updatedInstance match {
        case Failure(throwable) => Logger.error(s"Error updating instance: $throwable")
        case Success(changedInstance) => Logger.debug(s"Successfully applied an update to $changedInstance")
      }

      updatedInstance.flatMap(instanceStorage.writeInstance).map(addStatuses)
    }
  }

  private[this] def deleteInstance(id: String): Try[InstanceWithStatus] = {
    updateInstance(UpdateInstance(
      id = id,
      statusUpdater = Some(StatusUpdater(InstanceStatus.Stopped)),
      parameterValuesUpdater = None,
      templateSelector = None
    ))
    val instanceToDelete = instanceStorage.readInstance(id)
    val deletedInstance = instanceToDelete.flatMap(instanceStorage.deleteInstance)
    deletedInstance.map(addStatuses)
  }

}

object InstanceService {

  case object GetInstances

  case class GetInstance(id: String)

  case class NewInstance(instanceCreation: InstanceCreation)

  case class StatusUpdater(newStatus: InstanceStatus)

  case class ParameterValuesUpdater(newParameterValues: Map[String, String])

  case class TemplateSelector(newTemplateId: String)

  case class UpdateInstance(id: String,
                            statusUpdater: Option[StatusUpdater],
                            parameterValuesUpdater: Option[ParameterValuesUpdater],
                            templateSelector: Option[TemplateSelector])

  case class DeleteInstance(id: String)

  case class NomadStatuses(statuses: Map[String, InstanceStatus])

  case class ConsulServices(jobId: String, jobServices: Iterable[Service])

  case object NomadNotReachable

}
