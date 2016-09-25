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

  private val cancellable: Cancellable = system.scheduler.schedule(
    initialDelay = Duration.Zero,
    interval = Duration.create(1, TimeUnit.SECONDS),
    receiver = nomadActor,
    message = GetStatuses
  )(
    system.dispatcher,
    self
  )

  private val instancesFilePath = configuration.getString(conf.INSTANCES_FILE_KEY).getOrElse(conf.INSTANCES_FILE_DEFAULT)
  private val instancesFile = new File(instancesFilePath)
  private val instancesFileLockPath = instancesFilePath + ".lock"

  implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  @volatile
  private var instances: Map[String, Instance] = {
    Logger.info(s"Locking $instancesFilePath ($instancesFileLockPath)")
    val lock = new File(instancesFileLockPath)
    if (lock.createNewFile()) {
      sys.addShutdownHook{
        Logger.info(s"Releasing lock on $instancesFilePath ($instancesFileLockPath)")
        lock.delete()
      }
      Logger.info(s"Looking for instances in $instancesFilePath.")
      if (instancesFile.canRead && instancesFile.isFile) {
        val maybeInstances = InstanceService.loadInstances(new FileInputStream(instancesFile))
        if (maybeInstances.isFailure) {
          val throwable = maybeInstances.failed.get
          Logger.error(s"Error parsing instances in $instancesFilePath: $throwable")
          throw new IllegalStateException(throwable)
        } else {
          maybeInstances.get
        }
      } else {
        Logger.info(s"Instances file $instancesFilePath not found. Initializing with an empty instance collection.")
        InstanceService.persistInstances(Map.empty[String, Instance], new FileOutputStream(instancesFile))
      }
    } else {
      val error = s"Cannot lock $instancesFilePath. Is there another Broccoli instance running?"
      Logger.error(error)
      Await.ready(Play.current.stop(), Duration(10, TimeUnit.SECONDS))
      throw new IllegalStateException(error)
    }
  }

  def receive = {
    case GetInstances => sender ! instances.values
    case GetInstance(id) => sender ! instances.get(id)
    case NewInstance(instanceCreation) => sender() ! addInstance(instanceCreation)
    case updateInstanceInfo: UpdateInstance => sender() ! updateInstance(updateInstanceInfo)
    case DeleteInstance(id) => sender ! deleteInstance(id)
    case NomadStatuses(statuses) => updateStatusesBasedOnNomad(statuses)
    case ConsulServices(id, services) => updateServicesBasedOnNomad(id, services)
    case NomadNotReachable => setAllStatusesToUnknown()
  }

  private[this] def setAllStatusesToUnknown() = {
    instances.foreach {
      case (id, instance) => instance.status = InstanceStatus.Unknown
    }
  }

  private[this] def updateStatusesBasedOnNomad(statuses: Map[String, InstanceStatus]) = {
    Logger.debug(s"Received statuses: $statuses")
    instances.foreach { case (id, instance) =>
      statuses.get(id) match {
        case Some(nomadStatus) => Logger.debug(s"${instance.id}.status changed from ${instance.status} to $nomadStatus")
          instance.status = nomadStatus
          nomadActor ! GetServices(id)
        case None => Logger.debug(s"${instance.id}.status changed from ${instance.status} to ${InstanceStatus.Stopped}")
          instance.status = InstanceStatus.Stopped
          instance.services = Map.empty
      }
    }
  }

  private[this] def updateServicesBasedOnNomad(jobId: String, services: Iterable[Service]) = {
    Logger.debug(s"Received that job $jobId has the following services: ${services.map(_.name)}")
    instances.get(jobId) match {
      case Some(instance) => instance.services = services.map(service => (service.name, service)).toMap
      case None => Logger.error(s"Received services associated to non-existing job $jobId")
    }
  }

  private[this] def addInstance(instanceCreation: InstanceCreation): Try[Instance] = {
    Logger.info(s"Request received to create new instance: $instanceCreation")
    val maybeId = instanceCreation.parameters.get("id") // FIXME requires ID to be defined inside the parameter values
    val templateId = instanceCreation.templateId
    maybeId.map { id =>
      if (instances.contains(id)) {
        Failure(newExceptionWithWarning(new IllegalArgumentException(s"There is already an instance having the ID $id")))
      } else {
        val potentialTemplate = templateService.template(templateId)
        potentialTemplate.map { template =>
          val maybeNewInstance = Try(Instance(id, template, instanceCreation.parameters, InstanceStatus.Stopped, Map.empty))
          maybeNewInstance.map { newInstance =>
            instances = instances.updated(id, newInstance)
            InstanceService.persistInstances(instances, new FileOutputStream(instancesFile))
            newInstance
          }
        }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException(s"Template $templateId does not exist."))))
      }
    }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException("No ID specified"))))
  }

  private[this] def updateInstance(updateInstance: UpdateInstance): Option[Try[Instance]] = {
    val maybeInstance = instances.get(updateInstance.id)
    maybeInstance.map { instance =>
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

      InstanceService.persistInstances(instances, new FileOutputStream(instancesFile))
      updatedInstance
    }
  }

  private[this] def deleteInstance(id: String): Boolean = {
    updateInstance(UpdateInstance(
      id = id,
      statusUpdater = Some(StatusUpdater(InstanceStatus.Stopped)),
      parameterValuesUpdater = None,
      templateSelector = None
    ))
    if (instances.contains(id)) {
      instances = instances - id
      InstanceService.persistInstances(instances, new FileOutputStream(instancesFile))
      true
    } else {
      false
    }
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

  def persistInstances(instances: Map[String, Instance], output: OutputStream): Map[String, Instance] = {
    import Instance.instancePersistenceWrites
    val printStream = new PrintStream(output)
    printStream.append(Json.toJson(instances).toString())
    printStream.close()
    instances
  }

  def loadInstances(input: InputStream): Try[Map[String, Instance]] = {
    val source = Source.fromInputStream(input)
    val instances = Try(Json.parse(input).as[Map[String, Instance]])
    source.close()
    instances
  }

}
