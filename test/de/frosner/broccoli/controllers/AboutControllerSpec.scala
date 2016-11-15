package de.frosner.broccoli.controllers

import de.frosner.broccoli.services.{BuildInfoService, InstanceService, PermissionsService, SecurityService}
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, JsString}
import play.api.test._


class AboutControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "about" should {

    "return the about object" in new WithApplication {
      testWithAllAuths {
        securityService =>
          AboutController(
            buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
            instanceService = mock(classOf[InstanceService]),
            permissionsService = withDefaultPermissionsMode(mock(classOf[PermissionsService])),
            securityService = securityService
          )
      } {
        controller => controller.about
      } {
        identity
      } {
        (controller, result) => (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsObject(Map(
            "project" -> JsObject(Map(
              "name" -> JsString(controller.buildInfoService.projectName),
              "version" -> JsString(controller.buildInfoService.projectVersion)
            )),
            "scala" -> JsObject(Map(
              "version" -> JsString(controller.buildInfoService.scalaVersion)
            )),
            "sbt" -> JsObject(Map(
              "version" -> JsString(controller.buildInfoService.sbtVersion)
            )),
            "permissions" -> JsObject(Map(
              "mode" -> JsString(controller.permissionsService.getPermissionsMode())
            ))
          ))
        }
      }
    }

  }

}
