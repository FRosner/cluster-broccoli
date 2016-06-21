package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named, Singleton}

import akka.actor._
import de.frosner.broccoli.models._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.services.NomadService.{DeleteJob, GetStatuses, StartJob}
import de.frosner.broccoli.util.Logging
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import InstanceService._

@Singleton
class InstanceService @Inject()(templateService: TemplateService,
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
      parameterValues = Map("id" -> "zeppelin-frank"),
      status = InstanceStatus.Running,
      services = Map(
        "zeppelin-frank" -> Service(
          name = "zeppelin-frank-web-ui",
          protocol = "http",
          address = "localhost",
          port = 8000
        )
      )
    ),
    "zeppelin-pauline" -> Instance(
      id = "zeppelin-pauline",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "zeppelin-pauline"),
      status = InstanceStatus.Running,
      services = Map(
        "zeppelin-pauline" -> Service(
          name = "zeppelin-pauline-web-ui",
          protocol = "http",
          address = "localhost",
          port = 8000
        )
      )
    ),
    "jupyter-basil" -> Instance(
      id = "jupyter-basil",
      template = templateService.template("jupyter").get,
      parameterValues = Map("id" -> "jupyter-basil"),
      status = InstanceStatus.Stopped,
      services = Map(
        "jupyter-basil" -> Service(
          name = "jupyter-basil-web-ui",
          protocol = "http",
          address = "localhost",
          port = 8000
        )
      )
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
    Logger.debug(s"Received statuses: $statuses")
    instances.foreach { case (id, instance) =>
      statuses.get(id) match {
        case Some(nomadStatus) => Logger.debug(s"${instance.id}.status changed from ${instance.status} to $nomadStatus")
          instance.status = nomadStatus
        case None => Logger.debug(s"${instance.id}.status changed from ${instance.status} to ${InstanceStatus.Stopped}")
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
          val newInstance = Instance(id, template, instanceCreation.parameters, InstanceStatus.Stopped, Map.empty)
          instances = instances.updated(id, newInstance)
          Success(newInstance)
        }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException(s"Template $templateId does not exist."))))
      }
    }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException("No ID specified"))))
  }

  private[this] def setStatus(id: String, status: InstanceStatus): Option[Instance] = {
    // TODO tell the nomad actor to change the status
      val maybeInstance = instances.get(id)
      maybeInstance.flatMap { instance =>
        instance.status = InstanceStatus.Pending
        status match {
          case InstanceStatus.Running =>
            nomadActor.tell(StartJob(instance.templateJson), self)
            Some(instance)
          case InstanceStatus.Stopped =>
            nomadActor.tell(DeleteJob(instance.id), self)
            Some(instance)
          case other =>
            Logger.warn(s"Unsupported status change received: $other")
            None
        }
      }
//        maybeInstance.map { instance =>
//          instance.status = status
//          instance
//        }
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

