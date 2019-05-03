package de.frosner.broccoli.controllers

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import de.frosner.broccoli.auth.{Account, BroccoliSimpleAuthorization, DefaultEnv}
import de.frosner.broccoli.services._
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

case class AboutController @Inject()(
    aboutInfoService: AboutInfoService,
    override val securityService: SecurityService,
    override val cacheApi: SyncCacheApi,
    override val playEnv: Environment,
    override val silhouette: Silhouette[DefaultEnv],
    override val controllerComponents: ControllerComponents,
    override val executionContext: ExecutionContext
) extends BaseController
    with BroccoliSimpleAuthorization {

  def about = Action.async(parse.empty) { implicit request =>
    loggedIn { user =>
      Ok(Json.toJson(AboutController.about(aboutInfoService, user)))
    }
  }
}

object AboutController {

  def about(aboutInfoService: AboutInfoService, loggedIn: Account) =
    aboutInfoService.aboutInfo(loggedIn)

}
