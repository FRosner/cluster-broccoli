package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.{Instance}
import Instance.{instanceReads, instanceWrites}
import de.frosner.broccoli.services.InstanceService

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class InstanceController @Inject() (instanceService: InstanceService) extends Controller {

  private val instances = instanceService.instances

  def list = Action {
    Ok(Json.toJson(instances))
  }

  def show(id: String) = Action {
    instances.find(_.id == id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

}
