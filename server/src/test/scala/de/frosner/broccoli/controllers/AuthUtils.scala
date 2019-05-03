package de.frosner.broccoli.controllers

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api
import de.frosner.broccoli.auth.{Account, DefaultEnv, Role}
import de.frosner.broccoli.services.SecurityService
import org.specs2.matcher.MatchResult
import play.api.cache.SyncCacheApi
import play.api.mvc.{Action, AnyContentAsEmpty, Result}
import play.api.test._
import play.api.{Application, Environment}
import com.mohiva.play.silhouette.test._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthUtils extends ServiceMocks with PlaySpecification {

  def playEnv(implicit app: Application): Environment = app.injector.instanceOf[Environment]

  def cacheApi(implicit app: Application): SyncCacheApi = app.injector.instanceOf[SyncCacheApi]

  def testWithAllAuths[T <: AuthConfig, B](account: Account)(controller: (SecurityService, Account) => T)(
      action: T => Action[B])(requestModifier: FakeRequest[AnyContentAsEmpty.type] => FakeRequest[B])(
      matcher: (T, Future[Result]) => MatchResult[_]): MatchResult[_] = {
    val confAuthController = controller(withAuthConf(mock[SecurityService], List(account)), account)
    implicit val env: api.Environment[DefaultEnv] = withEnvironment(account)
    val login = new LoginInfo(account.name, account.name)
    val confAuthRequest = requestModifier(FakeRequest().withAuthenticator(login)(env))
    val confAuthResult = action(confAuthController).apply(confAuthRequest)
    val confAuthMatcher = matcher(confAuthController, confAuthResult)

    val noAuthController = controller(withAuthNone(mock[SecurityService]), Account.anonymous)
    val noAuthRequest = requestModifier(FakeRequest())
    val confAuthNoLoginResult = action(confAuthController).apply(noAuthRequest)
    val confAuthNoLoginMatcher = status(confAuthNoLoginResult) === 403

    confAuthMatcher and confAuthNoLoginMatcher
  }

  def testWithAllAuths[T <: AuthConfig, B](controller: (SecurityService, Account) => T)(action: T => Action[B])(
      requestModifier: FakeRequest[AnyContentAsEmpty.type] => FakeRequest[B])(
      matcher: (T, Future[Result]) => MatchResult[_]): MatchResult[_] = {
    val account = Account("user", ".*", Role.Administrator)
    val noAuthController = controller(withAuthNone(mock[SecurityService]), Account.anonymous)
    val noAuthRequest = requestModifier(FakeRequest())
    val noAuthResult = action(noAuthController).apply(noAuthRequest)
    val noAuthMatcher = matcher(noAuthController, noAuthResult)

    val authMatchers = testWithAllAuths(account)(controller)(action)(requestModifier)(matcher)

    noAuthMatcher and authMatchers
  }

}
