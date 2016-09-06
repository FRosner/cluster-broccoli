package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.build.BuildInfo
import de.frosner.broccoli.services.AboutService
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, Controller}

class AboutController @Inject()(aboutService: AboutService) extends Controller {

  def about: Action[AnyContent] = Action {
    Ok(JsObject(Map(
      "project" -> JsObject(Map(
        "name" -> JsString(aboutService.projectName),
        "version" -> JsString(aboutService.projectVersion)
      )),
      "scala" -> JsObject(Map(
        "version" -> JsString(aboutService.scalaVersion)
      )),
      "sbt" -> JsObject(Map(
        "version" -> JsString(aboutService.sbtVersion)
      ))
    )))
  }

}
