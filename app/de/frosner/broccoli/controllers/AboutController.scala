package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services._
import de.frosner.broccoli.conf
import jp.t2v.lab.play2.auth.BroccoliSimpleAuthorization
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, Controller}

case class AboutController @Inject()(buildInfoService: BuildInfoService,
                                     instanceService: InstanceService,
                                     nomadService: NomadService,
                                     consulService: ConsulService,
                                     override val securityService: SecurityService) extends Controller with BroccoliSimpleAuthorization {

  def about: Action[AnyContent] = StackAction { implicit request =>
    val user = loggedIn
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
      )),
      "auth" -> JsObject(Map(
        "enabled" -> JsBoolean(securityService.authMode != conf.AUTH_MODE_NONE),
        "user" -> JsObject(Map(
          "name" -> JsString(user.name),
          "role" -> JsString(user.role.toString),
          "instanceRegex" -> JsString(user.instanceRegex)
        ))
      )),
      "services" -> JsObject(Map(
        "clusterManager" -> JsObject(Map(
          "connected" -> JsBoolean(nomadService.isNomadReachable)
        )),
        "serviceDiscovery" -> JsObject(Map(
          "connected" -> JsBoolean(consulService.isConsulReachable)
        ))
      ))
    )))
  }

}
