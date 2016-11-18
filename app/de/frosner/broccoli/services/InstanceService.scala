package de.frosner.broccoli.services

import java.io._
import java.net.ConnectException
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
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class InstanceService @Inject()(templateService: TemplateService,
                                nomadService: NomadService,
                                consulService: ConsulService,
                                ws: WSClient,
                                applicationLifecycle: ApplicationLifecycle,
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
      nomadService.requestStatuses(instances.values.map(_.id).toSet)
    }
  }
  private val scheduledTask = scheduler.scheduleAtFixedRate(task, 0, pollingFrequencySeconds, TimeUnit.SECONDS)

  sys.addShutdownHook{
    scheduledTask.cancel(false)
    scheduler.shutdown()
  }

  private lazy val instanceStorage: InstanceStorage = {
    val instanceStorageType = {
      val storageType = configuration.getString(conf.INSTANCES_STORAGE_TYPE_KEY).getOrElse(conf.INSTANCES_STORAGE_TYPE_DEFAULT)
      val allowedStorageTypes = Set(conf.INSTANCES_STORAGE_TYPE_FS, conf.INSTANCES_STORAGE_TYPE_COUCHDB)
      if (!allowedStorageTypes.contains(storageType)) {
        Logger.error(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType is invalid. Only ${allowedStorageTypes.mkString(", ")} supported.")
        System.exit(1)
      }
      Logger.info(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType")
      storageType
    }

    val maybeInstanceStorage = instanceStorageType match {
      case conf.INSTANCES_STORAGE_TYPE_FS => {
        val instanceDir = {
          if (configuration.getString("broccoli.instancesFile").isDefined) Logger.warn(s"broccoli.instancesFile ignored. Use ${conf.INSTANCES_STORAGE_FS_URL_KEY} instead.")
          val url = configuration.getString(conf.INSTANCES_STORAGE_FS_URL_KEY).getOrElse(conf.INSTANCES_STORAGE_FS_URL_DEFAULT)
          Logger.info(s"${conf.INSTANCES_STORAGE_FS_URL_KEY}=$url")
          url
        }
        Try(FileSystemInstanceStorage(new File(instanceDir)))
      }
      case conf.INSTANCES_STORAGE_TYPE_COUCHDB => {
        val couchDbUrl = {
          val url = configuration.getString(conf.INSTANCES_STORAGE_COUCHDB_URL_KEY).getOrElse(conf.INSTANCES_STORAGE_COUCHDB_URL_DEFAULT)
          Logger.info(s"${conf.INSTANCES_STORAGE_COUCHDB_URL_KEY}=$url")
          url
        }
        val couchDbName = {
          val name = configuration.getString(conf.INSTANCES_STORAGE_COUCHDB_DBNAME_KEY).getOrElse(conf.INSTANCES_STORAGE_COUCHDB_DBNAME_DEFAULT)
          Logger.info(s"${conf.INSTANCES_STORAGE_COUCHDB_DBNAME_KEY}=$name")
          name
        }
        Try(CouchDBInstanceStorage(couchDbUrl, couchDbName, ws))
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
      Logger.info("Closing instanceStorage (shutdown hook)")
      instanceStorage.close()
    }
    applicationLifecycle.addStopHook(() => Future {
      Logger.info("Closing instanceStorage (stop hook)")
      instanceStorage.close()
    })
    instanceStorage
  }

  @volatile
  private var instancesMap: Map[String, Instance] = Map.empty
  @volatile
  private var instancesMapInitialized = false
  @volatile
  private def initializeInstancesMap: Map[String, Instance] = {
    instancesMapInitialized = true
    instancesMap = instanceStorage.readInstances match {
      case Success(instances) => instances.map(instance => (instance.id, instance)).toMap
      case Failure(throwable) => {
        Logger.error(s"Failed to load the instances: ${throwable.toString}")
        throw throwable
      }
    }
    instancesMap
  }

  /*
    I have to make this initialization "lazy" (but there are not lazy vars in Scala),
    because Play does not like it when you use System.exit(1) during the construction
    of dependency injected components.
   */
  @volatile
  private def instances = {
    if (instancesMapInitialized) {
      instancesMap
    } else {
      initializeInstancesMap
    }
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

  def getInstances: Iterable[InstanceWithStatus] = {
    instances.values.map(addStatuses)
  }

  def getInstance(id: String): Option[InstanceWithStatus] = {
    instances.get(id).map(addStatuses)
  }

  @volatile
  def addInstance(instanceCreation: InstanceCreation): Try[InstanceWithStatus] = {
    val maybeId = instanceCreation.parameters.get("id") // FIXME requires ID to be defined inside the parameter values
    val templateId = instanceCreation.templateId
    val maybeCreatedInstance = maybeId.map { id =>
      if (instances.contains(id)) {
        Failure(newExceptionWithWarning(new IllegalArgumentException(s"There is already an instance having the ID $id")))
      } else {
        val potentialTemplate = templateService.template(templateId)
        potentialTemplate.map { template =>
          val maybeNewInstance = Try(Instance(id, template, instanceCreation.parameters))
          maybeNewInstance.flatMap { newInstance =>
            val result = instanceStorage.writeInstance(newInstance)
            result.foreach { writtenInstance => instancesMap = instancesMap.updated(id, writtenInstance) }
            result
          }
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
    val maybeInstance = instances.get(id)
    val maybeUpdatedInstance = maybeInstance.map { instance =>
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

      updatedInstance.flatMap { instance =>
        val maybeWrittenInstance = instanceStorage.writeInstance(instance)
        maybeWrittenInstance.foreach(written => instancesMap = instancesMap.updated(id, written))
        maybeWrittenInstance
      }.map(addStatuses)
    }
    maybeUpdatedInstance match {
      case Some(tryUpdatedInstance) => tryUpdatedInstance
      case None => Failure(InstanceNotFoundException(id))
    }
  }

  @volatile
  def deleteInstance(id: String): Try[InstanceWithStatus] = {
    val tryStopping = updateInstance(
      id = id,
      statusUpdater = Some(StatusUpdater(InstanceStatus.Stopped)),
      parameterValuesUpdater = None,
      templateSelector = None
    )
    val tryDelete = tryStopping.map(_.instance).recover {
      case throwable: ConnectException => {
        // TODO #144
        Logger.warn(s"Failed to stop $id. It might be running when Nomad is reachable again.")
        instancesMap(id)
      }
    }.flatMap { stoppedInstance =>
      val tryDelete = instanceStorage.deleteInstance(stoppedInstance)
      tryDelete.foreach(instance => instancesMap -= instance.id)
      tryDelete
    }
    tryDelete.map(addStatuses)
  }

}

object InstanceService {

  case class StatusUpdater(newStatus: InstanceStatus)

  case class ParameterValuesUpdater(newParameterValues: Map[String, String])

  case class TemplateSelector(newTemplateId: String)

}
