package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit

import cats.instances.future._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{Account, DefaultEnv, Role}
import de.frosner.broccoli.services.{SecurityService, WebSocketService}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.mvc.MultipartFormData
import play.api.test.Helpers.stubControllerComponents
import play.api.test._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import de.frosner.broccoli.providers.PasswordProviderSpec
import com.mohiva.play.silhouette.test._

class SecurityControllerSpec extends PlaySpecification with AuthUtils with PasswordProviderSpec {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  val account = Account("frank", ".*", Role.Administrator)

  "verify" should {

    "work" in new WithApplication with Context {
      testWithAllAuths { (securityService, account) =>
        SecurityController(
          securityService,
          cacheApi,
          playEnv,
          stubControllerComponents(),
          withIdentities(account),
          mock[WebSocketService],
          provider,
          ExecutionContext.global
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

    "return 200 and a session ID in a cookie on successful login" in new WithApplication with Context {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        withIdentities(account),
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      (status(result) must be equalTo 200) and
        (cookies(result) should have length 1) and
        (cookies(result).head.name must be equalTo "id") and
        (cookies(result).head.value.length must beGreaterThan(0))
    }

    "return 401 when the POST request is valid but the authentication failed" in new WithApplication with Context {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        withIdentities(account),
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )

      controller.securityService.authenticate(Credentials(account.name, "password")) returns
        Future.successful(None)

      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      status(result) must be equalTo 401
    }

    "return 400 when the POST request is invalid" in new WithApplication with Context {
      val silhouette = withIdentities(account)
      implicit val env = silhouette.env
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        silhouette,
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val login = LoginInfo(CredentialsProvider.ID, account.name)
      val result = controller.login.apply(FakeRequest().withAuthenticator(login)(silhouette.env))
      status(result) must be equalTo 400
    }

    "cancel an existing websocket connection if already logged in" in new WithApplication with Context {
      val silhouette = withIdentities(account)
      implicit val env = silhouette.env
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        silhouette,
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val login = LoginInfo(account.name, account.name)
      val result = controller.login.apply(FakeRequest().withAuthenticator(login)(silhouette.env))
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService, timeout(5000)).closeConnections(Matchers.anyString())
    }

    "don't cancel anything if it is the first login" in new WithApplication with Context {
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        withIdentities(account),
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, "password"))
      val result = controller.login.apply(requestWithData)
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService, times(0)).closeConnections(Matchers.anyString())
    }

  }

  "logout" should {

    "return 200 and an empty cookie on successful logout" in new WithApplication with Context {
      val silhouette = withIdentities(account)
      implicit val env = silhouette.env
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        silhouette,
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val login = LoginInfo(account.name, account.name)
      val result = controller.logout.apply(FakeRequest().withAuthenticator(login)(env).withBody(()))
      (status(result) must be equalTo 200) and
        (cookies(result) should have size 1) and
        (cookies(result).head.name must be equalTo "id") and
        (cookies(result).head.value must be equalTo "")
    }

    "cancel the websocket connection" in new WithApplication with Context {
      val silhouette = withIdentities(account)
      implicit val env = silhouette.env
      val controller = SecurityController(
        withAuthConf(mock[SecurityService], List(account)),
        cacheApi,
        playEnv,
        stubControllerComponents(),
        silhouette,
        mock[WebSocketService],
        provider,
        ExecutionContext.global
      )
      val login = LoginInfo(account.name, account.name)
      val result = controller.logout.apply(FakeRequest().withAuthenticator(login)(silhouette.env).withBody(()))
      Await.ready(result, Duration(5, TimeUnit.SECONDS))
      verify(controller.webSocketService).closeConnections(Matchers.anyString())
    }
  }

  /**
    * The context.
    */
  trait Context extends BaseContext {

    /**
      * The test credentials.
      */
    lazy val credentials = Credentials("apollonia.vanova@watchmen.com", "s3cr3t")

    /**
      * The provider to test.
      */
    lazy val provider = new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }
}
