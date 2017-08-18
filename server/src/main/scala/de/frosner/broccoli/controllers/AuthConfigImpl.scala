package de.frosner.broccoli.controllers

import de.frosner.broccoli.models.{Account, Role}
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.auth._
import play.api.{Environment, Mode}
import play.api.cache.CacheApi
import play.api.libs.json.JsString
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait AuthConfigImpl extends AuthConfig {

  val securityService: SecurityService

  val playEnv: Environment

  val cacheApi: CacheApi

  type Id = String

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = scala.reflect.classTag[Id]

  val sessionTimeoutInSeconds = securityService.sessionTimeoutInSeconds

  val cookieSecure = securityService.cookieSecure

  override lazy val idContainer: AsyncIdContainer[Id] = securityService.allowMultiLogin match {
    case true  => AsyncIdContainer(new MultiLoginCacheIdContainer[Id](cacheApi))
    case false => AsyncIdContainer(new CacheIdContainer[Id])
  }

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] =
    Future.successful(securityService.getAccount(id))

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok(JsString("Login successful!"))) // the content is not used anyway as the controller replaces it

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok(JsString("Logout successful!")))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden("Authentication failed."))

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(
      implicit context: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden(
      s"Authorization failed: Your privileges (${user.role}) are not matching the required (${authority.getOrElse("None")})."))

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    user.role match {
      case Role.Administrator => true
      case Role.Operator      => authority == Role.Operator || authority == Role.User
      case Role.User          => authority == Role.User
    }
  }

  override lazy val tokenAccessor = new CookieTokenAccessor(
    cookieName = AuthConfigImpl.CookieName,
    cookieSecureOption = playEnv.mode == Mode.Prod && cookieSecure,
    cookieHttpOnlyOption = true,
    cookieDomainOption = None,
    cookiePathOption = "/",
    cookieMaxAge = Some(sessionTimeoutInSeconds)
  )

}

object AuthConfigImpl {

  val CookieName = "BROCCOLI_SESS_ID"

}
