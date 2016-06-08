package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import de.frosner.broccoli.models.{InstanceStatus, InstanceCreation, Instance}
import Instance.{instanceReads, instanceWrites}
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import de.frosner.broccoli.services.InstanceService
import play.api.Logger

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class InstanceController @Inject() (instanceService: InstanceService) extends Controller {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def listNomad(templateId: Option[String]) = Action.async {
    val maybeFilteredInstances = instanceService.nomadInstances.map(
      instances => templateId.map(
        id => instances.filter(_.template.id == id)
      ).getOrElse(instances)
    )
    maybeFilteredInstances.map(instances => Ok(Json.toJson(instances)))
  }

  def list(templateId: Option[String]) = Action {
    val instances = instanceService.getInstances
    val filteredInstances = templateId.map(
      id => instances.filter(_.template.id == id)
    ).getOrElse(instances)
    Ok(Json.toJson(filteredInstances))
  }

  def showNomad(id: String) = Action.async {
    instanceService.nomadInstance(id).map(_.map(instance => Ok(Json.toJson(instance))).getOrElse(NotFound))
  }

  def show(id: String) = Action {
    instanceService.getInstances.find(_.id == id).map(instance => Ok(Json.toJson(instance))).getOrElse(NotFound)
  }

  // TODO don't send ID in parameters but generate one instead
  def create = Action { request =>
    // TODO check if validate fails
    val maybeValidatedInstanceCreation = request.body.asJson.map(_.validate[InstanceCreation])
    maybeValidatedInstanceCreation.map { validatedInstanceCreation =>
      validatedInstanceCreation.map { instanceCreation =>
        val newInstance = instanceService.addInstance(instanceCreation)
        newInstance.map { instance =>
          Status(201)(Json.toJson(instance)).withHeaders(
            LOCATION -> s"/instances/${instance.id}" // TODO String constant
          )
        }.recover {
          case error => Status(400)(error.getMessage)
        }.get
      }.recoverTotal{ case error =>
        Status(400)("Invalid JSON format: " + error.toString)
      }
    }.getOrElse(Status(400)("Expected JSON data"))
  }

  def update(id: String) = Action { request =>
    // TODO check if validate fails
    val maybeValidatedExpectedInstanceStatus = request.body.asJson.map(_.validate[InstanceStatus])
    maybeValidatedExpectedInstanceStatus.map { validatedExpectedInstanceStatus =>
      validatedExpectedInstanceStatus.map { status =>
        if (status == InstanceStatus.Starting || status == InstanceStatus.Stopping) {
          val changedInstance = instanceService.setStatus(id, status)
          changedInstance.map(i => Ok(Json.toJson(i))).getOrElse(NotFound)
        } else {
          Status(400)(s"Status can only be set to ${InstanceStatus.Starting} or ${InstanceStatus.Stopping}")
        }
      }.recoverTotal { error =>
        Status(400)("Invalid JSON format: " + error.toString)
      }
    }.getOrElse(Status(400)("Expected JSON data"))
  }

}
