package de.frosner.broccoli.controllers

import java.io.FileNotFoundException
import javax.inject.Inject

import de.frosner.broccoli.models.InstanceStatusJson._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{Instance, InstanceCreation, InstanceStatus, InstanceWithStatus}
import de.frosner.broccoli.conf
import Instance.instanceApiWrites
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import de.frosner.broccoli.services.{InstanceService, PermissionsService, TemplateNotFoundException}
import de.frosner.broccoli.services.InstanceService._
import de.frosner.broccoli.util.Logging
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, Controller}

import scala.util.{Failure, Success}

class InstanceController @Inject() (instanceService: InstanceService,
                                    permissionsService: PermissionsService) extends Controller with Logging {

  def list(maybeTemplateId: Option[String]) = Action {
    val maybeInstances = instanceService.getInstances
    val maybeFilteredInstances = maybeTemplateId.map(
      id => maybeInstances.map(_.filter(_.instance.template.id == id))
    ).getOrElse(maybeInstances)
    val maybeAnonymizedInstances = if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      maybeFilteredInstances
    } else {
      maybeFilteredInstances.map(_.map(InstanceController.removeSecretVariables))
    }
    maybeAnonymizedInstances match {
      case Success(filteredInstances) => Ok(Json.toJson(filteredInstances))
      case Failure(throwable) => NotFound(throwable.toString)
    }
  }

  def show(id: String) = Action {
    val maybeInstance = instanceService.getInstance(id)
    maybeInstance.map {
      instance => Ok(Json.toJson{
        if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
          instance
        } else {
          InstanceController.removeSecretVariables(instance)
        }
      })
    }.recover {
      case throwable => NotFound(throwable.toString)
    }.get
  }

  def create = Action { request =>
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      val maybeValidatedInstanceCreation = request.body.asJson.map(_.validate[InstanceCreation])
      maybeValidatedInstanceCreation.map { validatedInstanceCreation =>
        validatedInstanceCreation.map { instanceCreation =>
          val tryNewInstance = instanceService.addInstance(instanceCreation)
          tryNewInstance.map { instanceWithStatus =>
            Status(201)(Json.toJson(instanceWithStatus)).withHeaders(
              LOCATION -> s"/api/v1/instances/${instanceWithStatus.instance.id}" // TODO String constant
            )
          }.recover { case error =>
            Status(400)("Invalid JSON format: " + error.toString)
          }.get
        }.getOrElse(Status(400)("Expected JSON data"))
      }.getOrElse(Status(400)("Expected JSON data"))
    } else {
      Status(403)(s"Broccoli must be running in '${conf.PERMISSIONS_MODE_ADMINISTRATOR}' " +
        s"permissions mode to allow the creation of instances.")
    }
  }


  def update(id: String) = Action { request =>
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_USER) {
      Status(403)(s"Updating instances now allowed when running in permissions mode '${conf.PERMISSIONS_MODE_USER}'.")
    } else {
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
          ParameterValuesUpdater(parameterValues.map { case (k, v) => (k, v.as[JsString].value) }.toMap)
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
          Status(400)("Invalid request to update an instance. Please refer to the API documentation.")
        } else if ((maybeParameterValuesUpdater.isDefined || maybeTemplateSelector.isDefined) &&
              permissionsService.getPermissionsMode != conf.PERMISSIONS_MODE_ADMINISTRATOR) {
          Status(403)(s"Updating parameter values or templates only available when running in " +
            s"'${conf.PERMISSIONS_MODE_ADMINISTRATOR}' mode.")
        } else {
          val maybeExistingAndChangedInstance = instanceService.updateInstance(
            id = id,
            statusUpdater = maybeStatusUpdater,
            parameterValuesUpdater = maybeParameterValuesUpdater,
            templateSelector = maybeTemplateSelector
          )
          maybeExistingAndChangedInstance match {
            case Success(changedInstance) => Ok(Json.toJson(changedInstance))
            case Failure(throwable: FileNotFoundException) => Status(404)(s"Instance not found: $throwable")
            case Failure(throwable: IllegalArgumentException) => Status(400)(s"Invalid request to update an instance: $throwable")
            case Failure(throwable: TemplateNotFoundException) => Status(400)(s"Invalid request to update an instance: $throwable")
            case Failure(throwable) => Status(500)(s"Something went wrong when updating the instance: $throwable")
          }
        }
      }.getOrElse(Status(400)("Expected JSON data"))
    }
  }

  def delete(id: String) = Action {
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      val maybeDeletedInstance = instanceService.deleteInstance(id)
      maybeDeletedInstance.map {
        instance => Ok(Json.toJson(instance))
      }.recover {
        case throwable => NotFound(throwable.toString)
      }.get
    } else {
      Status(403)(s"Instance deletion only allowed in '${conf.PERMISSIONS_MODE_ADMINISTRATOR}' mode.")
    }
  }

}

object InstanceController {

  def removeSecretVariables(instanceWithStatus: InstanceWithStatus): InstanceWithStatus = {
    // FIXME "censoring" through setting the values null is ugly but using Option[String] gives me stupid Json errors
    val instance = instanceWithStatus.instance
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val newParameterValues = instance.parameterValues.map { case (parameter, value) =>
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

}
