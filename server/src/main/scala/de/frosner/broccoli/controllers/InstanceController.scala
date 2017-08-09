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

  def list(maybeTemplateId: Option[String]): Action[Unit] = StackAction(parse.empty) { implicit request =>
    Results.Ok(Json.toJson(InstanceController.list(maybeTemplateId, loggedIn, instanceService)))
  }

  def show(id: String): Action[Unit] = StackAction(parse.empty) { implicit request =>
    val notFound = NotFound(s"Instance $id not found.")
    val user = loggedIn
    if (id.matches(user.instanceRegex)) {
      val maybeInstance = instanceService.getInstance(id)
      maybeInstance
        .map { instance =>
          Ok(Json.toJson(InstanceWithStatus.filterSecretsForRole(user.role)(instance)))
        }
        .getOrElse(notFound)
    } else {
      notFound
    }
  }

  def create: Action[InstanceCreation] = StackAction(parse.json[InstanceCreation]) { implicit request =>
    InstanceController.create(request.body, loggedIn, instanceService).toHTTPResult
  }

  def update(id: String): Action[InstanceUpdate] = StackAction(parse.json[InstanceUpdate]) { implicit request =>
    InstanceController.update(id, request.body, loggedIn, instanceService).toHTTPResult
  }

  def delete(id: String): Action[Unit] = StackAction(parse.empty) { implicit request =>
    InstanceController.delete(id, loggedIn, instanceService).toHTTPResult
  }

}

object InstanceController {

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

  def list(templateId: Option[String], loggedIn: Account, instanceService: InstanceService): Seq[InstanceWithStatus] =
    templateId
      .map(id => instanceService.getInstances.filter(_.instance.template.id == id))
      .getOrElse(instanceService.getInstances)
      .filter(_.instance.id.matches(loggedIn.instanceRegex))
      // Remove secrets from instances if the user may not see them
      .map(InstanceWithStatus.filterSecretsForRole(loggedIn.role))

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
