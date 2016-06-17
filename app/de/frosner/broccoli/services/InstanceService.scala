package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Named, Singleton, Inject}

import akka.actor._
import de.frosner.broccoli.models.InstanceStatus
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{InstanceStatus, InstanceCreation, Instance}
import NomadService.ListJobs
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
    interval = Duration.create(5, TimeUnit.SECONDS),
    receiver = nomadActor,
    message = ListJobs
  )(
    system.dispatcher,
    null
  )

  implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  @volatile
  private var instances = Map(
    "zeppelin-frank" -> Instance(
      id = "zeppelin-frank",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "frank"),
      status = InstanceStatus.Started
    ),
    "zeppelin-pauline" -> Instance(
      id = "zeppelin-pauline",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "pauline"),
      status = InstanceStatus.Starting
    ),
    "jupyter-basil" -> Instance(
      id = "jupyter-basil",
      template = templateService.template("jupyter").get,
      parameterValues = Map("id" -> "basil"),
      status = InstanceStatus.Stopped
    )
  )

  def receive = {
    case GetInstances => sender ! instances.values
    case GetInstance(id) => sender ! instances.get(id)
    case NewInstance(instanceCreation) => sender ! addInstance(instanceCreation)
    case SetStatus(id, status) => sender ! setStatus(id, status)
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
    val maybeInstance = instances.get(id)
    maybeInstance.map { instance =>
      instance.status = status
      instance
    }
  }

  // TODO ask nomad periodically for the current status and send a request according to the desired status

//  def nomadInstances: Future[Seq[Instance]] = {
//    val jobsRequest = ws.url(nomadBaseUrl + "/v1/jobs").withQueryString("prefix" -> nomadJobPrefix)
//    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
//    val jobsWithTemplate = jobsResponse.map(jsArray => {
//      val (ids, names) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Name").map(_.as[JsString].value))
//      ids.zip(names).flatMap{
//        case (id, name) => templateService.template(name).map(
//          template => Instance(id, template, Map("id" -> id), InstanceStatus.Unknown)
//        )
//      }
//    })
//    jobsWithTemplate
//  }

//  def nomadInstance(id: String): Future[Option[Instance]] = nomadInstances.map(_.find(_.id == id))

}

object InstanceService {
  case object GetInstances
  case class GetInstance(id: String)
  case class NewInstance(instanceCreation: InstanceCreation)
  case class SetStatus(id: String, status: InstanceStatus)
}

