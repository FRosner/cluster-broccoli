package de.frosner.broccoli.controllers

import de.frosner.broccoli.auth.Account.anonymous
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.services._
import play.api.libs.json.Json
import play.api.test.Helpers.stubControllerComponents
import play.api.test._

import scala.concurrent.ExecutionContext

class AboutControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "about" should {

    "return the about object with authentication" in new WithApplication {
      val account = Account("user", ".*", Role.Administrator)
      val aboutInfoService = withDummyValues(mock[AboutInfoService])
      testWithAllAuths(account) { (securityService, account) =>
        AboutController(aboutInfoService,
                        securityService,
                        cacheApi,
                        playEnv,
                        withIdentities(account),
                        stubControllerComponents(),
                        ExecutionContext.global)
      } { controller =>
        controller.about
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo Json.toJson(aboutInfoService.aboutInfo(account))
        }
      }
    }

    "return the about object without authentication" in new WithApplication {
      val account = anonymous
      val aboutInfoService = withDummyValues(mock[AboutInfoService])
      val controller =
        AboutController(aboutInfoService,
                        withAuthNone(mock[SecurityService]),
                        cacheApi,
                        playEnv,
                        withIdentities(account),
                        stubControllerComponents(),
                        ExecutionContext.global)
      val result = controller.about(FakeRequest().withBody(()))
      status(result) must be equalTo 200 and {
        contentAsJson(result) must be equalTo Json.toJson(aboutInfoService.aboutInfo(account))
      }
    }

  }

}
