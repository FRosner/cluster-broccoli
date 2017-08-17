package de.frosner.broccoli.services

import java.io._
import java.net.ConnectException
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad.NomadClient
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class InstanceService @Inject()(nomadClient: NomadClient,
                                templateService: TemplateService,
                                nomadService: NomadService,
                                consulService: ConsulService,
                                ws: WSClient,
                                applicationLifecycle: ApplicationLifecycle,
                                configuration: Configuration) {

  private val log = play.api.Logger(getClass)

  private lazy val pollingFrequencySecondsString = configuration.getString(conf.POLLING_FREQUENCY_KEY)
  private lazy val pollingFrequencySecondsTry = pollingFrequencySecondsString match {
    case Some(string) =>
      Try(string.toInt).flatMap { int =>
        if (int >= 1) Success(int) else Failure(new Exception())
      }
    case None => Success(conf.POLLING_FREQUENCY_DEFAULT)
  }
  if (pollingFrequencySecondsTry.isFailure) {
    log.error(
      s"Invalid ${conf.POLLING_FREQUENCY_KEY} specified: '${pollingFrequencySecondsString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  private lazy val pollingFrequencySeconds = pollingFrequencySecondsTry.get
  log.info(s"Nomad/Consul polling frequency set to $pollingFrequencySeconds seconds")

  private val scheduler = new ScheduledThreadPoolExecutor(1)
  private val task = new Runnable {
    def run() =
      nomadService.requestStatuses(instances.values.map(_.id).toSet)
  }
  private val scheduledTask = scheduler.scheduleAtFixedRate(task, 0, pollingFrequencySeconds, TimeUnit.SECONDS)

  sys.addShutdownHook {
    scheduledTask.cancel(false)
    scheduler.shutdown()
  }

  @volatile
  private lazy val instanceStorage: InstanceStorage = {
    val instanceStorageType = {
      val storageType =
        configuration.getString(conf.INSTANCES_STORAGE_TYPE_KEY).getOrElse(conf.INSTANCES_STORAGE_TYPE_DEFAULT)
      val allowedStorageTypes = Set(conf.INSTANCES_STORAGE_TYPE_FS, conf.INSTANCES_STORAGE_TYPE_COUCHDB)
      if (!allowedStorageTypes.contains(storageType)) {
        log.error(
          s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType is invalid. Only ${allowedStorageTypes.mkString(", ")} supported.")
        System.exit(1)
      }
      log.info(s"${conf.INSTANCES_STORAGE_TYPE_KEY}=$storageType")
      storageType
    }

    val maybeInstanceStorage = instanceStorageType match {
      case conf.INSTANCES_STORAGE_TYPE_FS => {
        val instanceDir = {
          if (configuration.getString("broccoli.instancesFile").isDefined)
            log.warn(s"broccoli.instancesFile ignored. Use ${conf.INSTANCES_STORAGE_FS_URL_KEY} instead.")
          val url =
            configuration.getString(conf.INSTANCES_STORAGE_FS_URL_KEY).getOrElse(conf.INSTANCES_STORAGE_FS_URL_DEFAULT)
          log.info(s"${conf.INSTANCES_STORAGE_FS_URL_KEY}=$url")
          url
        }
        Try(FileSystemInstanceStorage(new File(instanceDir)))
      }
      case conf.INSTANCES_STORAGE_TYPE_COUCHDB => {
        val couchDbUrl = {
          val url = configuration
            .getString(conf.INSTANCES_STORAGE_COUCHDB_URL_KEY)
            .getOrElse(conf.INSTANCES_STORAGE_COUCHDB_URL_DEFAULT)
          log.info(s"${conf.INSTANCES_STORAGE_COUCHDB_URL_KEY}=$url")
          url
        }
        val couchDbName = {
          val name = configuration
            .getString(conf.INSTANCES_STORAGE_COUCHDB_DBNAME_KEY)
            .getOrElse(conf.INSTANCES_STORAGE_COUCHDB_DBNAME_DEFAULT)
          log.info(s"${conf.INSTANCES_STORAGE_COUCHDB_DBNAME_KEY}=$name")
          name
        }
        Try(CouchDBInstanceStorage(couchDbUrl, couchDbName, ws))
      }
      case default => throw new IllegalStateException(s"Illegal storage type '$instanceStorageType")
    }
    val instanceStorage = maybeInstanceStorage match {
      case Success(storage) => storage
      case Failure(throwable) =>
        log.error(s"Cannot start instance storage: $throwable")
        System.exit(1)
        throw throwable
    }
    sys.addShutdownHook {
      log.info("Closing instanceStorage (shutdown hook)")
      if (!instanceStorage.isClosed) {
        instanceStorage.close()
      }
    }
    applicationLifecycle.addStopHook(() =>
      Future {
        log.info("Closing instanceStorage (stop hook)")
        if (!instanceStorage.isClosed) {
          instanceStorage.close()
        }
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
        log.error(s"Failed to load the instances: ${throwable.toString}")
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
  private def instances = synchronized {
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
      services = consulService.getServiceStatusesOrDefault(instanceId),
      periodicRuns = nomadService.getPeriodicRunsOrDefault(instanceId)
    )
  }

  def getInstances: Seq[InstanceWithStatus] =
    instances.values.map(addStatuses).toSeq

  def getInstance(id: String): Option[InstanceWithStatus] =
    instances.get(id).map(addStatuses)

  def addInstance(instanceCreation: InstanceCreation): Try[InstanceWithStatus] = synchronized {
    val parameters = instanceCreation.parameters.filter {
      case (parameter, value) => !value.isEmpty
    }
    val maybeId = parameters.get("id") // FIXME requires ID to be defined inside the parameter values
    val templateId = instanceCreation.templateId
    val maybeCreatedInstance = maybeId
      .map { id =>
        if (instances.contains(id)) {
          Failure(new IllegalArgumentException(s"There is already an instance having the ID $id"))
        } else {
          val potentialTemplate = templateService.template(templateId)
          potentialTemplate
            .map { template =>
              val maybeNewInstance = Try(Instance(id, template, parameters))
              maybeNewInstance.flatMap { newInstance =>
                val result = instanceStorage.writeInstance(newInstance)
                result.foreach { writtenInstance =>
                  instancesMap = instancesMap.updated(id, writtenInstance)
                }
                result
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
                     parameterValuesUpdater: Option[Map[String, String]],
                     templateSelector: Option[String]): Try[InstanceWithStatus] = synchronized {
    val updateInstanceId = id
    val parameterValuesUpdatesWithPossibleDefaults = parameterValuesUpdater.map { p =>
      p.filter {
        case (parameter, value) => !value.isEmpty
      }
    }
    val maybeInstance = instances.get(id)
    val maybeUpdatedInstance = maybeInstance.map { instance =>
      val instanceWithPotentiallyUpdatedTemplateAndParameterValues: Try[Instance] = if (templateSelector.isDefined) {
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
      val updatedInstance = instanceWithPotentiallyUpdatedTemplateAndParameterValues.map { instance =>
        statusUpdater
          .map {
            // Update the instance status
            case JobStatus.Running =>
              nomadService.startJob(instance.templateJson).map(_ => instance)
            case JobStatus.Stopped =>
              nomadService.deleteJob(instance.id).map(_ => instance)
            case other =>
              Failure(new IllegalArgumentException(s"Unsupported status change received: $other"))
          }
          .getOrElse {
            // Don't update the instance status
            Success(instance)
          }
      }.flatten

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
      templateSelector = None
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

  /**
    * Get all tasks of the given instance.
    *
    * In Nomad the hierarchy is normally "allocation -> tasks in that allocation".  However allocations have generic
    * UUID whereas tasks have human-readable names, so we believe that tasks are easier as an "entry point" for the user
    * in the UI.  Hence this method inverts the hierarchy of models returned by Nomad.
    *
    * @param user The user requesting tasks for the instance, for access control
    * @param id The instance ID
    * @return All tasks of the given instance with their allocations, or an empty list if the instance has no tasks or
    *         didn't exist.  If the user may not access the instance return an InstanceError instead.
    */
  def getInstanceTasks(user: Account)(id: String): EitherT[Future, InstanceError, InstanceTasks] =
    EitherT
      .pure[Future, InstanceError](id)
      .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex))(_.matches(user.instanceRegex))
      .semiflatMap(nomadClient.getAllocationsForJob)
      .map { allocations =>
        InstanceTasks(
          id,
          // Invert the order "allocation -> task" into "task -> allocation" (see doc comment)
          allocations.payload
            .flatMap(allocation => allocation.taskStates.mapValues(_ -> allocation))
            .groupBy {
              case (taskName, _) => taskName
            }
            .map {
              case (taskId, items) =>
                Task(taskId, items.map {
                  case (_, (events, allocation)) =>
                    Task.Allocation(allocation.id, allocation.clientStatus, events.state)
                })
            }
            .toSeq
        )
      }
}
