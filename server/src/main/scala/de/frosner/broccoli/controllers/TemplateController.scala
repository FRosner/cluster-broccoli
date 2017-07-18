package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.services.{SecurityService, TemplateService}
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}

case class TemplateController @Inject()(templateService: TemplateService, override val securityService: SecurityService)
    extends Controller
    with BroccoliSimpleAuthorization {

  private val templates = templateService.getTemplates

  def list: Action[AnyContent] = StackAction { implicit request =>
    Ok(Json.toJson(TemplateController.list(templateService)))
  }

  def show(id: String): Action[AnyContent] = StackAction { implicit request =>
    templateService.template(id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

}

object TemplateController {

  def list(templateService: TemplateService) =
    templateService.getTemplates

}
