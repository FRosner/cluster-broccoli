package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services.{BuildInfoService, InstanceService, SecurityService}
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, Controller}

case class AboutController @Inject()(buildInfoService: BuildInfoService,
                                     instanceService: InstanceService,
                                     override val securityService: SecurityService) extends Controller with BroccoliSimpleAuthorization {

  def about: Action[AnyContent] = StackAction { implicit request =>
    Ok(JsObject(Map(
      "project" -> JsObject(Map(
        "name" -> JsString(buildInfoService.projectName),
        "version" -> JsString(buildInfoService.projectVersion)
      )),
      "scala" -> JsObject(Map(
        "version" -> JsString(buildInfoService.scalaVersion)
      )),
      "sbt" -> JsObject(Map(
        "version" -> JsString(buildInfoService.sbtVersion)
      ))
    )))
  }

}
