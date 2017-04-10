package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.AboutInfo.aboutInfoWrites
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, Controller}

case class AboutController @Inject() ( aboutInfoService: AboutInfoService
                                     , override val securityService: SecurityService
                                     ) extends Controller with BroccoliSimpleAuthorization {

  def about: Action[AnyContent] = StackAction { implicit request =>
    Ok(Json.toJson(aboutInfoService.aboutInfo(loggedIn)))
  }

}
