package de.frosner.broccoli.controllers

import de.frosner.broccoli.services.{BuildInfoService, InstanceService, PermissionsService, SecurityService}
import jp.t2v.lab.play2.auth.test.Helpers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, JsString}
import play.api.test.{FakeRequest, PlaySpecification, _}

class AboutControllerSpec extends PlaySpecification with ServiceMocks {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "about" should {

    "return 200 if the user is authenticated" in new WithApplication {
      val account = UserAccount("frank", "pass")
      val controller = AboutController(
        buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
        instanceService = withEmptyInstancePrefix(mock(classOf[InstanceService])),
        permissionsService = withDefaultPermissionsMode(mock(classOf[PermissionsService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
      )

      val result = controller.about.apply(FakeRequest().withLoggedIn(controller)(account.name))
      (status(result) must be equalTo 200) and {
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
          )),
          "nomad" -> JsObject(Map(
            "jobPrefix" -> JsString(controller.instanceService.nomadJobPrefix)
          ))
        ))
      }
    }

    "return 403 if the user is not authenticated" in new WithApplication {
      val account = UserAccount("frank", "pass")
      val controller = AboutController(
        buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
        instanceService = withEmptyInstancePrefix(mock(classOf[InstanceService])),
        permissionsService = withDefaultPermissionsMode(mock(classOf[PermissionsService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
      )
      val result = controller.about.apply(FakeRequest())
      status(result) must be equalTo 403
    }

    "return 200 if auth is disabled" in {
      val controller = AboutController(
        buildInfoService = withDummyValues(mock(classOf[BuildInfoService])),
        instanceService = withEmptyInstancePrefix(mock(classOf[InstanceService])),
        permissionsService = withDefaultPermissionsMode(mock(classOf[PermissionsService])),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )

      val result = controller.about.apply(FakeRequest())
      (status(result) must be equalTo 200) and {
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
          )),
          "nomad" -> JsObject(Map(
            "jobPrefix" -> JsString(controller.instanceService.nomadJobPrefix)
          ))
        ))
      }
    }

  }

}
