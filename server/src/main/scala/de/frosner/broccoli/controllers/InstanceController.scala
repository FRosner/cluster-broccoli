package de.frosner.broccoli.controllers

import java.io.FileNotFoundException

import javax.inject.Inject
import cats.instances.future._
import cats.syntax.either._
import com.mohiva.play.silhouette.api.Silhouette
import de.frosner.broccoli.auth.{Account, BroccoliSimpleAuthorization, DefaultEnv, Role}
import de.frosner.broccoli.http.ToHTTPResult.ops._
import de.frosner.broccoli.instances.{InstanceNotFoundException, NomadInstances, PeriodicJobNotFoundException}
import de.frosner.broccoli.models.InstanceCreation.instanceCreationReads
import de.frosner.broccoli.models.InstanceUpdate.instanceUpdateReads
import de.frosner.broccoli.auth.Role.syntax._
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad.models.{Allocation, LogStreamKind, TaskLog, Task => NomadTask}
import de.frosner.broccoli.services._
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import shapeless.tag
import squants.information.Information

import scala.concurrent.ExecutionContext

case class InstanceController @Inject()(
    instances: NomadInstances,
    instanceService: InstanceService,
    override val securityService: SecurityService,
    override val cacheApi: SyncCacheApi,
    override val playEnv: Environment,
    override val silhouette: Silhouette[DefaultEnv],
    override val controllerComponents: ControllerComponents,
    override val executionContext: ExecutionContext
) extends BaseController
    with BroccoliSimpleAuthorization {

  def list(maybeTemplateId: Option[String]): Action[Unit] = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      Results.Ok(Json.toJson(InstanceController.list(maybeTemplateId, user, instanceService)))
    }
  }

  def show(id: String): Action[Unit] = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      Either
        .right[InstanceError, String](id)
        // Ensure that the user is allowed to access the instance based on its ID
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex))(_.matches(user.instanceRegex))
        .flatMap(id => instanceService.getInstance(id).toRight(InstanceError.NotFound(id)))
        .fold(_.toHTTPResult, instance => Ok(Json.toJson(instance.removeSecretsForRole(user.role))))
    }
  }

  def tasks(id: String): Action[Unit] = Action.async(parse.empty) { implicit request =>
    asyncLoggedIn { implicit user =>
      instances.getInstanceTasks(user)(id).value.map(_.toHTTPResult)
    }
  }

  /**
    * Serve a log file to the frontend.
    *
    * @param instanceId The ID of the instance
    * @param allocationId The ID of the allocation
    * @param taskName The name of the tasks whose logs to view
    * @param kind The kind of log
    * @param offset The offset from the end of the log to fetch
    * @return The log as plain text or an HTTP error
    */
  def logFile(
      instanceId: String,
      allocationId: String,
      taskName: String,
      kind: LogStreamKind,
      offset: Option[Information]
  ): Action[Unit] =
    Action.async(parse.empty) { implicit request =>
      asyncLoggedIn { user =>
        instances
          .getAllocationLog(user)(
            instanceId,
            tag[Allocation.Id](allocationId),
            tag[NomadTask.Name](taskName),
            kind,
            offset.map(tag[TaskLog.Offset](_))
          )
          .fold(_.toHTTPResult, Results.Ok(_))
      }
    }

  /**
    * Serve a log file to the frontend.
    *
    * @param instanceId The ID of the instance
    * @param allocationId The ID of the allocation
    * @param taskName The name of the tasks whose logs to view
    * @param kind The kind of log
    * @param offset The offset from the end of the log to fetch
    * @return The log as plain text or an HTTP error
    */
  def logFile(
      instanceId: String,
      periodicJobId: String,
      allocationId: String,
      taskName: String,
      kind: LogStreamKind,
      offset: Option[Information]
  ): Action[Unit] =
    Action.async(parse.empty) { implicit request =>
      asyncLoggedIn { user =>
        instances
          .getPeriodicJobAllocationLog(user)(
            instanceId,
            periodicJobId,
            tag[Allocation.Id](allocationId),
            tag[NomadTask.Name](taskName),
            kind,
            offset.map(tag[TaskLog.Offset](_))
          )
          .fold(a => a.toHTTPResult, Results.Ok(_))
      }
    }

  def create: Action[InstanceCreation] = Action.async(parse.json[InstanceCreation]) { implicit request =>
    loggedIn { implicit user =>
      InstanceController.create(request.body, user, instanceService).toHTTPResult
    }
  }

  def update(id: String): Action[InstanceUpdate] = Action.async(parse.json[InstanceUpdate]) { implicit request =>
    loggedIn { implicit user =>
      InstanceController.update(id, request.body, user, instanceService).toHTTPResult
    }
  }

  def delete(id: String): Action[Unit] = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      InstanceController.delete(id, user, instanceService).toHTTPResult
    }
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
        .map(idJson => Either.fromOption(idJson.validate[String].asOpt, InstanceError.IdMalformed: InstanceError))
        .joinRight
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
      .map(_.removeSecretsForRole(loggedIn.role))

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
          u.status.orElse(u.parameterValues).orElse(u.selectedTemplate).orElse(u.periodicJobsToStop).isDefined
        }
        .ensure(InstanceError.RolesRequired(Role.Administrator)) { u =>
          // Ensure that only administrators can update parameter values or instance templates
          user.role == Role.Administrator || (u.parameterValues.isEmpty && u.selectedTemplate.isEmpty)
        }
      updatedInstance <- Either
        .fromTry(
          instanceService.updateInstance(instanceId,
                                         update.status,
                                         update.parameterValues,
                                         update.selectedTemplate,
                                         update.periodicJobsToStop))
        .leftMap {
          case throwable: FileNotFoundException        => InstanceError.NotFound(instanceId, Some(throwable))
          case throwable: InstanceNotFoundException    => InstanceError.NotFound(instanceId, Some(throwable))
          case throwable: IllegalArgumentException     => InstanceError.InvalidParameters(throwable.getMessage)
          case throwable: TemplateNotFoundException    => InstanceError.TemplateNotFound(throwable.id)
          case throwable: PeriodicJobNotFoundException => InstanceError.InvalidParameters(throwable.getMessage)
          case throwable                               => InstanceError.Generic(throwable)
        }
      // Do not expose instance secrets to Operators
    } yield InstanceUpdated(update, updatedInstance.removeSecretsForRole(user.role))
}
