package de.frosner.broccoli.controllers

import de.frosner.broccoli.services.{BuildInfoService, InstanceService, SecurityService}
import de.frosner.broccoli.models.{UserAccount, Role, Anonymous}
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, JsString, JsBoolean}
import play.api.test._


class AboutControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  def baseJson(controller: AboutController) = Map(
    "project" -> JsObject(Map(
      "name" -> JsString(controller.buildInfoService.projectName),
      "version" -> JsString(controller.buildInfoService.projectVersion)
    )),
    "scala" -> JsObject(Map(
      "version" -> JsString(controller.buildInfoService.scalaVersion)
    )),
    "sbt" -> JsObject(Map(
      "version" -> JsString(controller.buildInfoService.sbtVersion)
    ))
  )

  "about" should {

    "return the about object with authentication" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      testWithAllAuths(account) {
        securityService =>
          AboutController(
            buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
            instanceService = mock(classOf[InstanceService]),
            securityService = securityService
          )
      } {
        controller => controller.about
      } {
        identity
      } {
        (controller, result) => (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsObject(baseJson(controller) ++ Map(
            "auth" -> JsObject(Map(
              "enabled" -> JsBoolean(true),
              "user" -> JsObject(Map(
                "name" -> JsString(account.name),
                "role" -> JsString(account.role.toString),
                "instanceRegex" -> JsString(account.instanceRegex)
              ))
            ))
          ))
        }
      }
    }

    "return the about object without authentication" in new WithApplication {
      val controller = AboutController(
        buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
        instanceService = mock(classOf[InstanceService]),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )
      val result = controller.about(FakeRequest())
      (status(result) must be equalTo 200) and {
        contentAsJson(result) must be equalTo JsObject(baseJson(controller) ++ Map(
          "auth" -> JsObject(Map(
            "enabled" -> JsBoolean(false),
            "user" -> JsObject(Map(
              "name" -> JsString(Anonymous.name),
              "role" -> JsString(Anonymous.role.toString),
              "instanceRegex" -> JsString(Anonymous.instanceRegex)
            ))
          ))
        ))
      }
    }

  }

}
