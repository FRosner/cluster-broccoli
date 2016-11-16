package de.frosner.broccoli.controllers

import java.io.FileNotFoundException
import javax.inject.Inject

import de.frosner.broccoli.models.InstanceStatusJson._
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{Instance, InstanceCreation, InstanceStatus, InstanceWithStatus}
import de.frosner.broccoli.conf
import Instance.instanceApiWrites
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import de.frosner.broccoli.services._
import de.frosner.broccoli.services.InstanceService._
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, Controller}

import scala.util.{Failure, Success, Try}

case class InstanceController @Inject() (instanceService: InstanceService,
                                         permissionsService: PermissionsService,
                                         override val securityService: SecurityService)
  extends Controller with Logging with BroccoliSimpleAuthorization {

  def list(maybeTemplateId: Option[String]) = StackAction { implicit request =>
    val user = loggedIn
    val instances = instanceService.getInstances
    val filteredInstances = maybeTemplateId.map(
      id => instances.filter(_.instance.template.id == id)
    ).getOrElse(instances).filter(_.instance.id.matches(user.instanceRegex))
    val anonymizedInstances = if (permissionsService.getPermissionsMode != conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      filteredInstances.map(InstanceController.removeSecretVariables)
    } else {
      filteredInstances
    }
    Ok(Json.toJson(anonymizedInstances))
  }

  def show(id: String) = StackAction { implicit request =>
    val notFound = NotFound(s"Instance $id not found.")
    if (id.matches(loggedIn.instanceRegex)) {
      val maybeInstance = instanceService.getInstance(id)
      maybeInstance.map {
        instance => Ok(Json.toJson {
          if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
            instance
          } else {
            InstanceController.removeSecretVariables(instance)
          }
        })
      }.getOrElse(notFound)
    } else {
      notFound
    }
  }

  def create = StackAction { implicit request =>
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      val maybeValidatedInstanceCreation = request.body.asJson.map(_.validate[InstanceCreation])
      maybeValidatedInstanceCreation.map { validatedInstanceCreation =>
        validatedInstanceCreation.map { instanceCreation =>
          val maybeId = instanceCreation.parameters.get("id")
          val instanceRegex = loggedIn.instanceRegex
          maybeId match {
            case Some(id) if id.matches(instanceRegex) =>
              val tryNewInstance = instanceService.addInstance(instanceCreation)
              tryNewInstance.map { instanceWithStatus =>
                Status(201)(Json.toJson(instanceWithStatus)).withHeaders(
                  LOCATION -> s"/api/v1/instances/${instanceWithStatus.instance.id}" // TODO String constant
                )
              }.recover { case error =>
                Status(400)(error.toString)
              }.get
            case Some(id) => Status(403)(s"Only allowed to create instances matching $instanceRegex")
            case None => Status(400)(s"Instance ID missing")
          }
        }.getOrElse(Status(400)("Expected JSON data"))
      }.getOrElse(Status(400)("Expected JSON data"))
    } else {
      Status(403)(s"Broccoli must be running in '${conf.PERMISSIONS_MODE_ADMINISTRATOR}' " +
        s"permissions mode to allow the creation of instances.")
    }
  }


  def update(id: String) = StackAction { implicit request =>
    val instanceRegex = loggedIn.instanceRegex
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_USER) {
      Status(403)(s"Updating instances now allowed when running in permissions mode '${conf.PERMISSIONS_MODE_USER}'.")
    } else if (!id.matches(instanceRegex)) {
      Status(403)(s"Only allowed to update instances matching $instanceRegex")
    } else {
      val maybeJsObject = request.body.asJson.map(_.as[JsObject])
      maybeJsObject.map { jsObject =>
        // extract updates
        val fields = jsObject.value
        val maybeStatusUpdater = fields.get("status").flatMap { value =>
          val maybeValidatedNewStatus = Try(value.as[InstanceStatus])
          maybeValidatedNewStatus.map(status => Some(StatusUpdater(status))).getOrElse(None)
        }
        val maybeParameterValuesUpdater = Try{
          fields.get("parameterValues").map { value =>
            val parameterValues = value.as[JsObject].value
            ParameterValuesUpdater(parameterValues.map { case (k, v) => (k, v.as[JsString].value) }.toMap)
          }
        }.toOption.getOrElse(None)
        val maybeTemplateSelector = Try(fields.get("selectedTemplate").map { value =>
          TemplateSelector(value.as[JsString].value)
        }).toOption.getOrElse(None)

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
            case Failure(throwable: InstanceNotFoundException) => Status(404)(s"Instance not found: $throwable")
            case Failure(throwable: IllegalArgumentException) => Status(400)(s"Invalid request to update an instance: $throwable")
            case Failure(throwable: TemplateNotFoundException) => Status(400)(s"Invalid request to update an instance: $throwable")
            case Failure(throwable) => Status(500)(s"Something went wrong when updating the instance: $throwable")
          }
        }
      }.getOrElse(Status(400)("Expected JSON data"))
    }
  }

  def delete(id: String) = StackAction { implicit request =>
    if (permissionsService.getPermissionsMode == conf.PERMISSIONS_MODE_ADMINISTRATOR) {
      val instanceRegex = loggedIn.instanceRegex
      if (id.matches(instanceRegex)) {
        val maybeDeletedInstance = instanceService.deleteInstance(id)
        maybeDeletedInstance.map {
          instance => Ok(Json.toJson(instance))
        }.recover {
          case throwable: InstanceNotFoundException => NotFound(throwable.toString)
          case throwable => Status(400)(throwable.toString)
        }.get
      } else {
        Status(403)(s"Only allowed to delete instances matching $instanceRegex")
      }
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
