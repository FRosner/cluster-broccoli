package de.frosner.broccoli.controllers

import com.mohiva.play.silhouette.api.Silhouette
import de.frosner.broccoli.auth.{BroccoliSimpleAuthorization, DefaultEnv}
import javax.inject.Inject
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.services.{SecurityService, TemplateService}
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

case class TemplateController @Inject()(
    templateService: TemplateService,
    override val securityService: SecurityService,
    override val playEnv: Environment,
    override val cacheApi: SyncCacheApi,
    override val controllerComponents: ControllerComponents,
    override val executionContext: ExecutionContext,
    override val silhouette: Silhouette[DefaultEnv]
) extends BaseController
    with BroccoliSimpleAuthorization {

  def list = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      Ok(Json.toJson(TemplateController.list(templateService)))
    }
  }

  def show(id: String) = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      templateService.template(id).map(template => Ok(Json.toJson(template))).getOrElse(NotFound)
    }
  }

}

object TemplateController {

  def list(templateService: TemplateService): Seq[Template] =
    templateService.getTemplates

}
