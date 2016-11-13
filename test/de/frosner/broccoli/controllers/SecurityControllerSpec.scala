package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.auth.test.Helpers._
import org.mockito.Mockito._
import play.api.mvc.MultipartFormData
import play.api.test.{FakeRequest, PlaySpecification, _}

class SecurityControllerSpec extends PlaySpecification {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  def securityController(accounts: Iterable[Account]): SecurityController = {
    val securityService = mock(classOf[SecurityService])
    accounts.foreach { account =>
      when(securityService.getAccount(account.name)).thenReturn(Some(account))
    }
    SecurityController(securityService)
  }

  "verify" should {

    "return 200 if the user is authenticated" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      val result = controller.verify.apply(FakeRequest().withLoggedIn(controller)(account.name))
      status(result) must be equalTo 200
    }

    "return 403 if the user is not authenticated but allowed" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      val result = controller.verify.apply(FakeRequest())
      status(result) must be equalTo 403
    }

  }

  "login" should {

    def loginFormData[T](userName: String, password: String): MultipartFormData[T] = MultipartFormData(
      dataParts = Map(
        SecurityController.UsernameFormKey -> Seq(userName),
        SecurityController.PasswordFormKey -> Seq(password)
      ),
      files = Seq.empty,
      badParts = Seq.empty,
      missingFileParts = Seq.empty
    )


    "return 200 and a session ID in a cookie on successful login" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      when(controller.securityService.isAllowedToAuthenticate(account)).thenReturn(true)
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, account.password))
      val result = controller.login.apply(requestWithData)
      (status(result) must be equalTo 200) and
        (header("Set-Cookie", result) should beSome.which((s: String) => s.matches(s"${AuthConfigImpl.CookieName}=.+; .*")))
    }

    "return 401 when the POST request is valid but the authentication failed" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      val requestWithData = FakeRequest().withMultipartFormDataBody(loginFormData(account.name, account.password))
      val result = controller.login.apply(requestWithData)
      status(result) must be equalTo 401
    }

    "return 400 when the POST request is invalid" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      val result = controller.login.apply(FakeRequest().withLoggedIn(controller)(account.name))
      status(result) must be equalTo 400
    }

  }

  "logout" should {

    "return 200 and an empty cookie on successful logout" in new WithApplication {
      val account = Account("frank", "pass")
      val controller = securityController(List(account))
      val result = controller.logout.apply(FakeRequest().withLoggedIn(controller)(account.name))
      (status(result) must be equalTo 200) and
        (header("Set-Cookie", result) should beSome.which((s: String) => s.startsWith(s"${AuthConfigImpl.CookieName}=; ")))
    }

  }

}
