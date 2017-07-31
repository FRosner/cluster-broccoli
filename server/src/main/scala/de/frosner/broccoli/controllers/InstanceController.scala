package de.frosner.broccoli.controllers

import java.io.FileNotFoundException
import javax.inject.Inject

import de.frosner.broccoli.models.JobStatusJson._
import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models._
import de.frosner.broccoli.conf
import Instance.instanceApiWrites
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import InstanceUpdate.{instanceUpdateReads, instanceUpdateWrites}
import de.frosner.broccoli.services._
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{Action, Controller, Results}
import play.mvc.Http.HeaderNames

import scala.util.{Failure, Success, Try}

case class InstanceController @Inject()(instanceService: InstanceService, override val securityService: SecurityService)
    extends Controller
    with Logging
    with BroccoliSimpleAuthorization {

  def list(maybeTemplateId: Option[String]) = StackAction { implicit request =>
    val instances = InstanceController.list(maybeTemplateId, loggedIn, instanceService)
    Results.Ok(Json.toJson(instances))
  }

  def show(id: String) = StackAction { implicit request =>
    val notFound = NotFound(s"Instance $id not found.")
    val user = loggedIn
    if (id.matches(user.instanceRegex)) {
      val maybeInstance = instanceService.getInstance(id)
      maybeInstance
        .map { instance =>
          Ok(Json.toJson {
            if (user.role == Role.Administrator) {
              instance
            } else {
              InstanceController.removeSecretVariables(instance)
            }
          })
        }
        .getOrElse(notFound)
    } else {
      notFound
    }
  }

  def create = StackAction { implicit request =>
    val maybeValidatedInstanceCreation = request.body.asJson.map(_.validate[InstanceCreation])
    maybeValidatedInstanceCreation
      .map { validatedInstanceCreation =>
        validatedInstanceCreation
          .map { instanceCreation =>
            InstanceController.create(instanceCreation, loggedIn, instanceService) match {
              case InstanceCreationSuccess(creation, instanceWithStatus) =>
                Results
                  .Status(201)(Json.toJson(instanceWithStatus))
                  .withHeaders(
                    HeaderNames.LOCATION -> s"/api/v1/instances/${instanceWithStatus.instance.id}" // TODO String constant
                  )
              case InstanceCreationFailure(creation, reason) =>
                Results.Status(400)(s"Creating $instanceCreation failed: $reason")
            }
          }
          .getOrElse(Results.Status(400)("Expected JSON data"))
      }
      .getOrElse(Results.Status(400)("Expected JSON data"))
  }

  def update(id: String) = StackAction { implicit request =>
    val maybeJsObject = request.body.asJson.map(_.as[JsObject])
    val maybeInstanceUpdate = request.body.asJson.map(_.validate[InstanceUpdate])
    maybeInstanceUpdate
      .map { instanceUpdateResult =>
        instanceUpdateResult
          .map { instanceUpdate =>
            InstanceController.update(id, instanceUpdate, loggedIn, instanceService) match {
              case InstanceUpdateSuccess(update, updatedInstance) =>
                Results.Ok(Json.toJson(updatedInstance))
              case InstanceUpdateFailure(update, reason) =>
                Results.Status(400)(s"Updating instance $id failed: $reason")
            }
          }
          .getOrElse(Status(400)("Expected JSON data"))
      }
      .getOrElse(Status(400)("Expected JSON data"))
  }

  def delete(id: String) = StackAction { implicit request =>
    InstanceController.delete(id, loggedIn, instanceService) match {
      case InstanceDeletionSuccess(id, instanceWithStatus) =>
        Results.Ok(Json.toJson(instanceWithStatus))
      case InstanceDeletionFailure(id, reason) =>
        Results.BadRequest(s"Deleting $id failed: $reason")
    }
  }

}

object InstanceController {

  def removeSecretVariables(instanceWithStatus: InstanceWithStatus): InstanceWithStatus = {
    // FIXME "censoring" through setting the values null is ugly but using Option[String] gives me stupid Json errors
    val instance = instanceWithStatus.instance
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val newParameterValues = instance.parameterValues.map {
      case (parameter, value) =>
        val possiblyCensoredValue = if (parameterInfos.get(parameter).exists(_.secret == Some(true))) {
          null.asInstanceOf[String]
        } else {
          value
        }
        (parameter, possiblyCensoredValue)
    }
    instanceWithStatus.copy(
      instance = instanceWithStatus.instance.copy(
        parameterValues = newParameterValues
      )
    )
  }

  def create(instanceCreation: InstanceCreation,
             loggedIn: Account,
             instanceService: InstanceService): InstanceCreationResult =
    if (loggedIn.role == Role.Administrator) {
      val maybeId = instanceCreation.parameters.get("id")
      val instanceRegex = loggedIn.instanceRegex
      maybeId match {
        case Some(id) if id.matches(instanceRegex) =>
          val tryNewInstance = instanceService.addInstance(instanceCreation)
          tryNewInstance
            .map { instanceWithStatus =>
              InstanceCreationSuccess(instanceCreation, instanceWithStatus)
            }
            .recover {
              case error =>
                InstanceCreationFailure(instanceCreation, error.toString)
            }
            .get
        case Some(id) =>
          InstanceCreationFailure(
            instanceCreation,
            s"Only allowed to create instances matching $instanceRegex"
          )
        case None =>
          InstanceCreationFailure(
            instanceCreation,
            s"Instance ID missing"
          )
      }
    } else {
      InstanceCreationFailure(
        instanceCreation,
        s"Only administrators are allowed to create new instances"
      )
    }

  def delete(id: String, loggedIn: Account, instanceService: InstanceService): InstanceDeletionResult =
    if (loggedIn.role == Role.Administrator) {
      val instanceRegex = loggedIn.instanceRegex
      if (id.matches(instanceRegex)) {
        val maybeDeletedInstance = instanceService.deleteInstance(id)
        maybeDeletedInstance
          .map { instance =>
            InstanceDeletionSuccess(id, instance)
          }
          .recover {
            case throwable => InstanceDeletionFailure(id, throwable.toString)
          }
          .get
      } else {
        InstanceDeletionFailure(id, s"Only allowed to delete instances matching $instanceRegex")
      }
    } else {
      InstanceDeletionFailure(
        id,
        s"Only administrators are allowed to delete instances"
      )
    }

  def list(templateId: Option[String], loggedIn: Account, instanceService: InstanceService): Seq[InstanceWithStatus] = {
    val filteredInstances = templateId
      .map(
        id => instanceService.getInstances.filter(_.instance.template.id == id)
      )
      .getOrElse(instanceService.getInstances)
      .filter(_.instance.id.matches(loggedIn.instanceRegex))
    val anonymizedInstances = if (loggedIn.role != Role.Administrator) {
      filteredInstances.map(removeSecretVariables)
    } else {
      filteredInstances
    }
    anonymizedInstances
  }

  def update(id: String,
             instanceUpdate: InstanceUpdate,
             loggedIn: Account,
             instanceService: InstanceService): InstanceUpdateResult = {
    val user = loggedIn
    if (user.role == Role.Administrator || user.role == Role.Operator) {
      val instanceRegex = user.instanceRegex
      if (!id.matches(instanceRegex)) {
        InstanceUpdateFailure(instanceUpdate, s"Only allowed to update instances matching $instanceRegex")
      } else {
        val maybeStatusUpdater = instanceUpdate.status
        val maybeParameterValuesUpdater = instanceUpdate.parameterValues
        val maybeTemplateSelector = instanceUpdate.selectedTemplate
        if (maybeStatusUpdater.isEmpty && maybeParameterValuesUpdater.isEmpty && maybeTemplateSelector.isEmpty) {
          InstanceUpdateFailure(instanceUpdate,
                                "Invalid request to update an instance. Please refer to the API documentation.")
        } else if ((maybeParameterValuesUpdater.isDefined || maybeTemplateSelector.isDefined) && user.role != Role.Administrator) {
          InstanceUpdateFailure(instanceUpdate,
                                s"Updating parameter values or templates only allowed for administrators.")
        } else {
          val maybeExistingAndChangedInstance = instanceService.updateInstance(
            id = id,
            statusUpdater = maybeStatusUpdater,
            parameterValuesUpdater = maybeParameterValuesUpdater,
            templateSelector = maybeTemplateSelector
          )
          maybeExistingAndChangedInstance match {
            case Success(changedInstance) => InstanceUpdateSuccess(instanceUpdate, changedInstance)
            case Failure(throwable: FileNotFoundException) =>
              InstanceUpdateFailure(instanceUpdate, s"Instance not found: $throwable")
            case Failure(throwable: InstanceNotFoundException) =>
              InstanceUpdateFailure(instanceUpdate, s"Instance not found: $throwable")
            case Failure(throwable: IllegalArgumentException) =>
              InstanceUpdateFailure(instanceUpdate, s"Invalid request to update an instance: $throwable")
            case Failure(throwable: TemplateNotFoundException) =>
              InstanceUpdateFailure(instanceUpdate, s"Invalid request to update an instance: $throwable")
            case Failure(throwable) =>
              InstanceUpdateFailure(instanceUpdate, s"Something went wrong when updating the instance: $throwable")
          }
        }
      }
    } else {
      InstanceUpdateFailure(
        instanceUpdate,
        s"Only administrators and operators are allowed to update instances"
      )
    }
  }

}
