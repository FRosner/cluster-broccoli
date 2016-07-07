package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.Template
import Template.templateWrites
import de.frosner.broccoli.services.TemplateService
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}


class TemplateController @Inject() (templateService: TemplateService) extends Controller {

  private val templates = templateService.templates

  def list: Action[AnyContent] = Action {
    Ok(Json.toJson(templateService.templates))
  }

  def show(id: String): Action[AnyContent] = Action {
    templateService.template(id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

}
