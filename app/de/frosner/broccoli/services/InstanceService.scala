package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Named, Singleton, Inject}

import akka.actor._
import de.frosner.broccoli.models.InstanceStatus
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{InstanceStatus, InstanceCreation, Instance}
import de.frosner.broccoli.services.NomadService.GetStatuses
import de.frosner.broccoli.util.Logging
import play.api.{Logger, Configuration}
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure, Try}

import InstanceService._

@Singleton
class InstanceService @Inject()(configuration: Configuration,
                                templateService: TemplateService,
                                system: ActorSystem,
                                @Named("nomad-actor") nomadActor: ActorRef,
                                ws: WSClient) extends Actor with Logging {

  private val cancellable: Cancellable = system.scheduler.schedule(
    initialDelay = Duration.Zero,
    interval = Duration.create(1, TimeUnit.SECONDS),
    receiver = nomadActor,
    message = GetStatuses
  )(
    system.dispatcher,
    self
  )

  implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  @volatile
  private var instances = Map(
    "zeppelin-frank" -> Instance(
      id = "zeppelin-frank",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "jupyter-frank"),
      status = InstanceStatus.Running
    ),
    "zeppelin-pauline" -> Instance(
      id = "zeppelin-pauline",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "jupyter-pauline"),
      status = InstanceStatus.Running
    ),
    "jupyter-basil" -> Instance(
      id = "jupyter-basil",
      template = templateService.template("jupyter").get,
      parameterValues = Map("id" -> "jupyter-basil"),
      status = InstanceStatus.Stopped
    )
  )

  def receive = {
    case GetInstances => sender ! instances.values
    case GetInstance(id) => sender ! instances.get(id)
    case NewInstance(instanceCreation) => sender() ! addInstance(instanceCreation)
    case SetStatus(id, status) => sender() ! setStatus(id, status)
    case NomadStatuses(statuses) => updateStatusesBasedOnNomad(statuses)
    case NomadNotReachable => setAllStatusesToUnknown()
  }

  private[this] def setAllStatusesToUnknown() = {
    instances.foreach {
      case (id, instance) => instance.status = InstanceStatus.Unknown
    }
  }

  private[this] def updateStatusesBasedOnNomad(statuses: Map[String, InstanceStatus]) = {
    Logger.info(s"Received statuses: $statuses")
    instances.foreach { case (id, instance) =>
      statuses.get(id) match {
        case Some(nomadStatus) => Logger.info(s"${instance.id}.status changed from ${instance.status} to $nomadStatus")
          instance.status = nomadStatus
        case None => Logger.info(s"${instance.id}.status changed from ${instance.status} to ${InstanceStatus.Stopped}")
          instance.status = InstanceStatus.Stopped
      }
    }
  }

  private[this] def addInstance(instanceCreation: InstanceCreation): Try[Instance] = {
    Logger.info(s"Request received to create new instance: $instanceCreation")
    val maybeId = instanceCreation.parameters.get("id")
    val templateId = instanceCreation.templateId
    maybeId.map { id =>
      if (instances.contains(id)) {
        Failure(newExceptionWithWarning(new IllegalArgumentException(s"There is already an instance having the ID $id")))
      } else {
        val potentialTemplate = templateService.template(templateId)
        potentialTemplate.map { template =>
          val newInstance = Instance(id, template, instanceCreation.parameters, InstanceStatus.Stopped)
          instances = instances.updated(id, newInstance)
          Success(newInstance)
        }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException(s"Template $templateId does not exist."))))
      }
    }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException("No ID specified"))))
  }

  private[this] def setStatus(id: String, status: InstanceStatus): Option[Instance] = {
    // TODO tell the nomad actor to change the status
    val maybeInstance = instances.get(id)
    maybeInstance.map { instance =>
      instance.status = status
      instance
    }
  }


}

object InstanceService {
  case object GetInstances
  case class GetInstance(id: String)
  case class NewInstance(instanceCreation: InstanceCreation)
  case class SetStatus(id: String, status: InstanceStatus)
  case class NomadStatuses(statuses: Map[String, InstanceStatus])
  case object NomadNotReachable
}

