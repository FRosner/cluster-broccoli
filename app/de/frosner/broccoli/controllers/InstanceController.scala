package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.{Instance}
import Instance.{instanceReads, instanceWrites}

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

// class InstanceController @Inject() (ws: WSClient) extends Controller {
class InstanceController extends Controller {

  def list = Action {
    Ok(Json.toJson(Instance.instances))
  }

  def show(id: String) = Action {
    Ok(Json.toJson(Instance.instances.find(_.id == id).get))
  }

}
