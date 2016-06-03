package de.frosner.broccoli.controllers

import java.io.File
import javax.inject.Inject

import de.frosner.broccoli.models.Template
import Template.{templateReads, templateWrites}
import de.frosner.broccoli.services.TemplateService
import play.{Logger, Play, Application}

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.io.Source
import scala.util.Try

//class TemplateController @Inject() (ws: WSClient) extends Controller {
class TemplateController @Inject() (configuration: play.api.Configuration, templateService: TemplateService) extends Controller {

  private val templates = templateService.templates

  def list = Action {
    Ok(Json.toJson(templateService.templates.map(_.id)))
  }

  def show(id: String) = Action {
    templates.find(_.id == id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

}
