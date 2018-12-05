package de.frosner.broccoli.controllers

import de.frosner.broccoli.auth.Account
import javax.inject.Inject
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.services.{SecurityService, TemplateService}
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.Environment
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}

case class TemplateController @Inject()(
    templateService: TemplateService,
    override val securityService: SecurityService,
    override val playEnv: Environment,
    override val cacheApi: CacheApi
) extends Controller
    with BroccoliSimpleAuthorization {

  def list = StackAction(parse.empty) { implicit request =>
      Ok(Json.toJson(TemplateController.list(templateService)))
  }

  def show(id: String) = StackAction(parse.empty) { implicit request =>
    templateService.template(id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
  }

}

object TemplateController {

  def list(templateService: TemplateService): Seq[Template] =
    templateService.getTemplates

}
