package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.{Instance}
import Instance.{instanceReads, instanceWrites}
import de.frosner.broccoli.services.InstanceService

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

// class InstanceController @Inject() (ws: WSClient) extends Controller {
class InstanceController @Inject() (instanceService: InstanceService) extends Controller {

  // TODO start implementation: https://www.playframework.com/documentation/2.5.x/ScalaWS

  def list = Action {
    Ok(Json.toJson(instanceService.instances))
  }

  def show(id: String) = Action {
    Ok(Json.toJson(instanceService.instances.find(_.id == id).get))
  }

}
