package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.Template
import Template.{templateReads, templateWrites}

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

//class TemplateController @Inject() (ws: WSClient) extends Controller {
class TemplateController extends Controller {

  def list = Action {
    Ok(Json.toJson(Template.templates))
  }

  def show(id: String) = Action {
    Ok(Json.toJson(Template.templates.find(_.id == id).get))
  }

  // TODO start implementation: https://www.playframework.com/documentation/2.5.x/ScalaWS

}
