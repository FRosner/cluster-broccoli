package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.AboutInfo.aboutInfoWrites
import de.frosner.broccoli.models.Account
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.Environment
import play.api.cache.CacheApi
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, Controller}

case class AboutController @Inject()(
    aboutInfoService: AboutInfoService,
    override val securityService: SecurityService,
    override val cacheApi: CacheApi,
    override val playEnv: Environment
) extends Controller
    with BroccoliSimpleAuthorization {

  def about = StackAction(parse.empty) { implicit request =>
    Ok(Json.toJson(AboutController.about(aboutInfoService, loggedIn)))
  }

}

object AboutController {

  def about(aboutInfoService: AboutInfoService, loggedIn: Account) =
    aboutInfoService.aboutInfo(loggedIn)

}
