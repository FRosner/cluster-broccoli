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
import cats.syntax.either._
import de.frosner.broccoli.services._
import de.frosner.broccoli.http.ToHTTPResult.ops._
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{Action, Controller, Request, Results}
import play.mvc.Http.HeaderNames

import scala.util.{Failure, Success, Try}

case class InstanceController @Inject()(instanceService: InstanceService, override val securityService: SecurityService)
    extends Controller
    with Logging
    with BroccoliSimpleAuthorization {

  def list(maybeTemplateId: Option[String]) = StackAction(parse.empty) { implicit request =>
    Results.Ok(Json.toJson(InstanceController.list(maybeTemplateId, loggedIn, instanceService)))
  }

  def show(id: String) = StackAction(parse.empty) { implicit request =>
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

  def create = StackAction(parse.json[InstanceCreation]) { implicit request =>
    InstanceController
      .create(request.body, loggedIn, instanceService)
      .fold(
        _.toHTTPResult,
        created =>
          Results
            .Created(Json.toJson(created.instanceWithStatus))
            .withHeaders(HeaderNames.LOCATION -> s"/api/v1/instances/${created.instanceWithStatus.instance.id}")
      )
  }

  def update(id: String) = StackAction(parse.json[InstanceUpdate]) { implicit request =>
    InstanceController
      .update(id, request.body, loggedIn, instanceService)
      .fold(_.toHTTPResult, updated => Results.Ok(Json.toJson(updated.instanceWithStatus)))
  }

  def delete(id: String) = StackAction(parse.empty) { implicit request =>
    InstanceController
      .delete(id, loggedIn, instanceService)
      .fold(_.toHTTPResult, deleted => Results.Ok(Json.toJson(deleted.instance)))
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
             instanceService: InstanceService): Either[InstanceError, InstanceCreated] =
    for {
      user <- Either
        .right[InstanceError, Account](loggedIn)
        .ensure(InstanceError.RolesRequired(Role.Administrator): InstanceError)(_.role == Role.Administrator)
      _ <- Either
        .fromOption(instanceCreation.parameters.get("id"), InstanceError.IdMissing: InstanceError)
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex): InstanceError)(_.matches(user.instanceRegex))
      newInstance <- Either
        .fromTry(instanceService.addInstance(instanceCreation))
        .leftMap(InstanceError.Generic(_): InstanceError)
    } yield InstanceCreated(instanceCreation, newInstance)

  def delete(
      id: String,
      loggedIn: Account,
      instanceService: InstanceService
  ): Either[InstanceError, InstanceDeleted] =
    for {
      user <- Either
        .right[InstanceError, Account](loggedIn)
        .ensure(InstanceError.RolesRequired(Role.Administrator): InstanceError)(_.role == Role.Administrator)
      instanceId <- Either
        .right[InstanceError, String](id)
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex): InstanceError)(_.matches(user.instanceRegex))
      deletedInstance <- Either
        .fromTry(instanceService.deleteInstance(instanceId))
        .leftMap(InstanceError.Generic(_): InstanceError)
    } yield InstanceDeleted(instanceId, deletedInstance)

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

  def update(
      id: String,
      instanceUpdate: InstanceUpdate,
      loggedIn: Account,
      instanceService: InstanceService
  ): Either[InstanceError, InstanceUpdated] =
    for {
      user <- Either
        .right[InstanceError, Account](loggedIn)
        .ensure(InstanceError.RolesRequired(Role.Administrator, Role.Operator))(u =>
          u.role == Role.Administrator || u.role == Role.Operator)
      instanceId <- Either
        .right[InstanceError, String](id)
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex))(_.matches(user.instanceRegex))
      update <- Either
        .right[InstanceError, InstanceUpdate](instanceUpdate)
        .ensure(InstanceError.InvalidParameters(
          "Invalid request to update an instance. Please refer to the API documentation.")) { u =>
          // Ensure that at least one parameter of the status update is set
          u.status.orElse(u.parameterValues).orElse(u.selectedTemplate).isDefined
        }
        .ensure(InstanceError.RolesRequired(Role.Administrator)) { u =>
          // Ensure that only administrators can update parameter values or instance templates
          user.role == Role.Administrator || (u.parameterValues.isEmpty && u.selectedTemplate.isEmpty)
        }
      updatedInstance <- Either
        .fromTry(instanceService.updateInstance(id, update.status, update.parameterValues, update.selectedTemplate))
        .leftMap {
          case throwable: FileNotFoundException     => InstanceError.NotFound(id, Some(throwable))
          case throwable: InstanceNotFoundException => InstanceError.NotFound(id, Some(throwable))
          case throwable: IllegalArgumentException  => InstanceError.InvalidParameters(throwable.getMessage)
          case throwable: TemplateNotFoundException => InstanceError.TemplateNotFound(throwable.id)
          case throwable                            => InstanceError.Generic(throwable)
        }
    } yield InstanceUpdated(update, updatedInstance)
}
