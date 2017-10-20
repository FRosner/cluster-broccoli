package de.frosner.broccoli.controllers

import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.auth.test.Helpers._
import org.mockito.Mockito._
import org.specs2.matcher.MatchResult
import org.specs2.matcher.MatchersImplicits._
import play.api.cache.CacheApi
import play.api.mvc.{Action, AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, Environment}

import scala.concurrent.Future

trait AuthUtils extends ServiceMocks {

  def playEnv(implicit app: Application): Environment = app.injector.instanceOf[Environment]

  def cacheApi(implicit app: Application): CacheApi = app.injector.instanceOf[CacheApi]

  def testWithAllAuths[T <: AuthConfigImpl, B](account: Account)(controller: SecurityService => T)(
      action: T => Action[B])(requestModifier: FakeRequest[AnyContentAsEmpty.type] => FakeRequest[B])(
      matcher: (T, Future[Result]) => MatchResult[_]): MatchResult[_] = {
    val confAuthController = controller(withAuthConf(mock[SecurityService], List(account)))
    val confAuthRequest = requestModifier(FakeRequest().withLoggedIn(confAuthController)(account.name))
    val confAuthResult = action(confAuthController).apply(confAuthRequest)
    val confAuthMatcher = matcher(confAuthController, confAuthResult)

    val noAuthController = controller(withAuthNone(mock[SecurityService]))
    val noAuthRequest = requestModifier(FakeRequest())
    val confAuthNoLoginResult = action(confAuthController).apply(noAuthRequest)
    val confAuthNoLoginMatcher = status(confAuthNoLoginResult) === 403

    confAuthMatcher and confAuthNoLoginMatcher
  }

  def testWithAllAuths[T <: AuthConfigImpl, B](controller: SecurityService => T)(action: T => Action[B])(
      requestModifier: FakeRequest[AnyContentAsEmpty.type] => FakeRequest[B])(
      matcher: (T, Future[Result]) => MatchResult[_]): MatchResult[_] = {
    val account = Account("user", ".*", Role.Administrator)
    val noAuthController = controller(withAuthNone(mock[SecurityService]))
    val noAuthRequest = requestModifier(FakeRequest())
    val noAuthResult = action(noAuthController).apply(noAuthRequest)
    val noAuthMatcher = matcher(noAuthController, noAuthResult)

    val authMatchers = testWithAllAuths(account)(controller)(action)(requestModifier)(matcher)

    noAuthMatcher and authMatchers
  }

}
