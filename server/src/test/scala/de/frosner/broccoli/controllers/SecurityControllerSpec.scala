package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit

import cats.data.OptionT
import cats.instances.future._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.Credentials
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.services.{SecurityService, WebSocketService}
import jp.t2v.lab.play2.auth.test.Helpers._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.mvc.MultipartFormData
import play.api.test._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class SecurityControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  val account = Account("frank", ".*", Role.Administrator)

  "verify" should {

    "work" in new WithApplication {
      testWithAllAuths { securityService =>
        SecurityController(
          securityService,
          cacheApi,
          playEnv,
          mock[WebSocketService]
        )
      } { controller =>
        controller.verify
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 200
      }
    }

  }

  "login" should {

    def loginFormData[T](userName: String, password: String): MultipartFormData[T] = MultipartFormData(
      dataParts = Map(
        SecurityController.UsernameFormKey -> Seq(userName),
        SecurityController.PasswordFormKey -> Seq(password)
      ),
      files = Seq.empty,
      badParts = Seq.empty
    )

    "return 200 and a session ID in a cookie on successful login" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      (status(result) must be equalTo 200) and
        (header("Set-Cookie", result) should beSome.which((s: String) =>
          s.matches(s"${AuthConfigImpl.CookieName}=.+; .*")))
    }

    "return 401 when the POST request is valid but the authentication failed" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )

      controller.securityService.authenticate(Credentials(account.name, "password")) returns
        Future.successful(None)

      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      status(result) must be equalTo 401
    }

    "return 400 when the POST request is invalid" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val result = controller.login.apply(FakeRequest().withLoggedIn(controller)(account.name))
      status(result) must be equalTo 400
    }

    "cancel an existing websocket connection if already logged in" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val result = controller.login.apply(FakeRequest().withLoggedIn(controller)(account.name))
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService).closeConnections(Matchers.anyString())
    }

    "don't cancel anything if it is the first login" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService, times(0)).closeConnections(Matchers.anyString())
    }

  }

  "logout" should {

    "return 200 and an empty cookie on successful logout" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val result = controller.logout.apply(FakeRequest().withLoggedIn(controller)(account.name).withBody(()))
      (status(result) must be equalTo 200) and
        (header("Set-Cookie", result) should beSome.which((s: String) =>
          s.startsWith(s"${AuthConfigImpl.CookieName}=; ")))
    }

    "cancel the websocket connection" in new WithApplication {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        mock[WebSocketService]
      )
      val result = controller.logout.apply(FakeRequest().withLoggedIn(controller)(account.name).withBody(()))
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService).closeConnections(Matchers.anyString())
    }
  }
}
