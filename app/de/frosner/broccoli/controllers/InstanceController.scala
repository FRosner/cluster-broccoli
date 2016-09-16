package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import de.frosner.broccoli.models.InstanceStatusJson._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{Instance, InstanceCreation, InstanceStatus}
import Instance.instanceWrites
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import de.frosner.broccoli.services.InstanceService
import de.frosner.broccoli.services.InstanceService._
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

class InstanceController @Inject() (@Named("instance-actor") instanceService: ActorRef) extends Controller {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val timeout: Timeout = Duration.create(5, TimeUnit.SECONDS)

  def list(maybeTemplateId: Option[String]) = Action.async {
    val eventuallyInstances = instanceService.ask(GetInstances).mapTo[Iterable[Instance]]
    val eventuallyFilteredInstances = maybeTemplateId.map(
      id => eventuallyInstances.map(_.filter(_.template.id == id))
    ).getOrElse(eventuallyInstances)
    eventuallyFilteredInstances.map(filteredInstances => Ok(Json.toJson(filteredInstances)))
  }

  def show(id: String) = Action.async {
    val eventuallyMaybeInstance = instanceService.ask(GetInstance(id)).mapTo[Option[Instance]]
    eventuallyMaybeInstance.map(_.find(_.id == id).map(instance => Ok(Json.toJson(instance))).getOrElse(NotFound))
  }

  def create = Action.async { request =>
    val maybeValidatedInstanceCreation = request.body.asJson.map(_.validate[InstanceCreation])
    maybeValidatedInstanceCreation.map { validatedInstanceCreation =>
      validatedInstanceCreation.map { instanceCreation =>
        val eventuallyNewInstance = instanceService.ask(NewInstance(instanceCreation)).mapTo[Try[Instance]]
        eventuallyNewInstance.map { newInstance =>
          newInstance.map { instance =>
            Status(201)(Json.toJson(instance)).withHeaders(
              LOCATION -> s"/api/v1/instances/${instance.id}" // TODO String constant
            )
          }.recover {
            case error => Status(400)(error.getMessage)
          }.get
        }
      }.recoverTotal { case error =>
        Future(Status(400)("Invalid JSON format: " + error.toString))
      }
    }.getOrElse(Future(Status(400)("Expected JSON data")))
  }

  def update(id: String) = Action.async { request =>
    val maybeJsObject = request.body.asJson.map(_.as[JsObject])
    maybeJsObject.map { jsObject =>
      // extract updates
      val fields = jsObject.value
      val maybeStatusUpdater = fields.get("status").flatMap { value =>
        val maybeValidatedNewStatus = value.validate[InstanceStatus]
        maybeValidatedNewStatus.map(status => Some(StatusUpdater(status))).getOrElse(None)
      }
      val maybeParameterValuesUpdater = fields.get("parameterValues").map { value =>
        val parameterValues = value.as[JsObject].value
        ParameterValuesUpdater(parameterValues.map{ case (k, v) => (k, v.as[JsString].value) }.toMap)
      }
      val maybeTemplateSelector = fields.get("selectedTemplate").map { value =>
        TemplateSelector(value.as[JsString].value)
      }

      // warn for unrecognized updates
      fields.foreach { case (key, _) =>
        if (!Set("status", "parameterValues", "selectedTemplate").contains(key))
          Logger.warn(s"Received unrecognized instance update field: $key")
      }
      if (maybeStatusUpdater.isEmpty && maybeParameterValuesUpdater.isEmpty && maybeTemplateSelector.isEmpty) {
        Future(Status(400)("Invalid request to update an instance. Please refer to the API documentation."))
      } else {
        val update = UpdateInstance(
          id = id,
          statusUpdater = maybeStatusUpdater,
          parameterValuesUpdater = maybeParameterValuesUpdater,
          templateSelector = maybeTemplateSelector
        )
        val eventuallyMaybeExistingAndChangedInstance = instanceService.ask(update).mapTo[Option[Try[Instance]]]
        eventuallyMaybeExistingAndChangedInstance.map { maybeExistingAndChangedInstance =>
          maybeExistingAndChangedInstance.map { maybeChangedInstance =>
            if (maybeChangedInstance.isSuccess) {
              Ok(Json.toJson(maybeChangedInstance.get))
            } else {
              Status(400)(s"Invalid request to update an instance: ${maybeChangedInstance.failed.get}")
            }
          }.getOrElse(NotFound)
        }
      }
    }.getOrElse(Future(Status(400)("Expected JSON data")))
  }

  def delete(id: String) = Action.async {
    val eventuallyDeletedInstance = instanceService.ask(DeleteInstance(id)).mapTo[Boolean]
    eventuallyDeletedInstance.map { deleted =>
      if (deleted)
        Ok
      else
        NotFound
    }
  }

}
