package de.frosner.broccoli.services

import java.io._
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.models._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success, Try}
import InstanceService._
import de.frosner.broccoli.conf

@Singleton
class InstanceService @Inject()(templateService: TemplateService,
                                nomadService: NomadService,
                                consulService: ConsulService,
                                ws: WSClient,
                                configuration: Configuration) extends Logging {

  private lazy val pollingFrequencySecondsString = configuration.getString(conf.POLLING_FREQUENCY_KEY)
  private lazy val pollingFrequencySecondsTry = pollingFrequencySecondsString match {
    case Some(string) => Try(string.toInt).flatMap {
      int => if (int >= 1) Success(int) else Failure(new Exception())
    }
    case None => Success(conf.POLLING_FREQUENCY_DEFAULT)
  }
  if (pollingFrequencySecondsTry.isFailure) {
    Logger.error(s"Invalid ${conf.POLLING_FREQUENCY_KEY} specified: '${pollingFrequencySecondsString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  private lazy val pollingFrequencySeconds = pollingFrequencySecondsTry.get
  Logger.info(s"Nomad/Consul polling frequency set to $pollingFrequencySeconds seconds")

  private val scheduler = new ScheduledThreadPoolExecutor(1)
  private val task = new Runnable {
    def run() = {
      nomadService.requestStatuses()
    }
  }
  private val scheduledTask = scheduler.scheduleAtFixedRate(task, 0, pollingFrequencySeconds, TimeUnit.SECONDS)

  sys.addShutdownHook{
    scheduledTask.cancel(false)
    scheduler.shutdown()
  }

  lazy val nomadJobPrefix = {
    val prefix = conf.getNomadJobPrefix(configuration)
    Logger.info(s"${conf.NOMAD_JOB_PREFIX_KEY}=$prefix")
    prefix
  }

  private lazy val instanceStorage: InstanceStorage = {
    val instanceStorageType = {
      val storageType = configuration.getString(conf.INSTANCES_STORAGE_TYPE_KEY).getOrElse(conf.TEMPLATES_STORAGE_TYPE_DEFAULT)
      val allowedStorageTypes = Set(conf.INSTANCES_STORAGE_TYPE_FS, conf.INSTANCES_STORAGE_TYPE_COUCHDB)
      if (!allowedStorageTypes.contains(storageType)) {
        Logger.error(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType is invalid. Only ${allowedStorageTypes.mkString(", ")} supported.")
        System.exit(1)
      }
      Logger.info(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType")
      storageType
    }

    val instanceStorageUrl = {
      if (configuration.getString("broccoli.instancesFile").isDefined) Logger.warn(s"broccoli.instancesFile ignored. Use ${conf.INSTANCES_STORAGE_URL_KEY} instead.")
      val url = configuration.getString(conf.INSTANCES_STORAGE_URL_KEY).getOrElse { instanceStorageType match {
        case conf.INSTANCES_STORAGE_TYPE_FS => conf.INSTANCES_STORAGE_URL_DEFAULT_FS
        case conf.INSTANCES_STORAGE_TYPE_COUCHDB => conf.INSTANCES_STORAGE_URL_DEFAULT_COUCHDB
      }}
      Logger.info(s"${conf.INSTANCES_STORAGE_URL_KEY}=$url")
      url
    }

    val maybeInstanceStorage = instanceStorageType match {
      case conf.INSTANCES_STORAGE_TYPE_FS => {
        Try(FileSystemInstanceStorage(new File(instanceStorageUrl), nomadJobPrefix))
      }
      case conf.INSTANCES_STORAGE_TYPE_COUCHDB => {
        Try(CouchDBInstanceStorage(instanceStorageUrl, nomadJobPrefix, ws))
      }
      case default => throw new IllegalStateException(s"Illegal storage type '$instanceStorageType")
    }
    val instanceStorage = maybeInstanceStorage match {
      case Success(storage) => storage
      case Failure(throwable) =>
        Logger.error(s"Cannot start instance storage: $throwable")
        System.exit(1)
        throw throwable
    }
    sys.addShutdownHook {
      Logger.info("Closing instanceStorage")
      instanceStorage.close()
    }
    instanceStorage
  }

  private def addStatuses(instance: Instance): InstanceWithStatus = {
    val instanceId = instance.id
    InstanceWithStatus(
      instance = instance,
      status = nomadService.getJobStatusOrDefault(instanceId),
      services = consulService.getServiceStatusesOrDefault(instanceId)
    )
  }

  implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def getInstances: Try[Iterable[InstanceWithStatus]] = {
    instanceStorage.readInstances.map(_.map(addStatuses))
  }

  def getInstance(id: String): Try[InstanceWithStatus] = {
    instanceStorage.readInstance(id).map(addStatuses)
  }

  @volatile
  def addInstance(instanceCreation: InstanceCreation): Try[InstanceWithStatus] = {
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

  def updateInstance(id: String,
                     statusUpdater: Option[StatusUpdater],
                     parameterValuesUpdater: Option[ParameterValuesUpdater],
                     templateSelector: Option[TemplateSelector]): Try[InstanceWithStatus] = {
    val updateInstanceId = id
    val maybeInstance = instanceStorage.readInstance(updateInstanceId)
    maybeInstance.flatMap { instance =>
      val instanceWithPotentiallyUpdatedTemplateAndParameterValues: Try[Instance] = if (templateSelector.isDefined) {
        val newTemplateId = templateSelector.get.newTemplateId
        val newTemplate = templateService.template(newTemplateId)
        newTemplate.map { template =>
          // Requested template exists, update the template
          if (parameterValuesUpdater.isDefined) {
            // New parameter values are specified
            val newParameterValues = parameterValuesUpdater.get.newParameterValues
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
        if (parameterValuesUpdater.isDefined) {
          // Just update the parameter values
          val newParameterValues = parameterValuesUpdater.get.newParameterValues
          instance.updateParameterValues(newParameterValues)
        } else {
          // Neither template update nor parameter value update required
          Success(instance)
        }
      }
      val updatedInstance = instanceWithPotentiallyUpdatedTemplateAndParameterValues.map { instance =>
        statusUpdater.map {
          // Update the instance status
          case StatusUpdater(InstanceStatus.Running) =>
            nomadService.startJob(instance.templateJson).map(_ => instance)
          case StatusUpdater(InstanceStatus.Stopped) =>
            nomadService.deleteJob(instance.id).map(_ => instance)
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

  @volatile
  def deleteInstance(id: String): Try[InstanceWithStatus] = {
    updateInstance(
      id = id,
      statusUpdater = Some(StatusUpdater(InstanceStatus.Stopped)),
      parameterValuesUpdater = None,
      templateSelector = None
    )
    val instanceToDelete = instanceStorage.readInstance(id)
    val deletedInstance = instanceToDelete.flatMap(instanceStorage.deleteInstance)
    deletedInstance.map(addStatuses)
  }

}

object InstanceService {

  case class StatusUpdater(newStatus: InstanceStatus)

  case class ParameterValuesUpdater(newParameterValues: Map[String, String])

  case class TemplateSelector(newTemplateId: String)

}
