package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.Role.Role
import de.frosner.broccoli.models.{Account, Role}
import de.frosner.broccoli.services.SecurityService
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth._
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait AuthConfigImpl extends AuthConfig with Logging {

  val securityService: SecurityService

  type Id = String

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = scala.reflect.classTag[Id]

  val sessionTimeoutInSeconds = securityService.sessionTimeoutInSeconds

  val cookieSecure = securityService.cookieSecure

  override lazy val idContainer: AsyncIdContainer[Id] = securityService.allowMultiLogin match {
    case true => AsyncIdContainer(new MultiLoginCacheIdContainer[Id])
    case false => AsyncIdContainer(new CacheIdContainer[Id])
  }

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Future.successful(securityService.getAccount(id))

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok("Login successful."))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok("Logout successful."))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden("Authentication failed."))

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Results.Forbidden(s"Authorization failed: Your privileges (${user.role}) are not matching the required (${authority.getOrElse("None")})."))
  }

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    user.role match {
      case Role.Administrator => true
      case Role.Operator => authority == Role.Operator || authority == Role.NormalUser
      case Role.NormalUser => authority == Role.NormalUser
    }
  }

  override lazy val tokenAccessor = new CookieTokenAccessor(
    cookieName = AuthConfigImpl.CookieName,
    cookieSecureOption = play.api.Play.maybeApplication.exists(app => play.api.Play.isProd(app) && cookieSecure),
    cookieHttpOnlyOption = true,
    cookieDomainOption = None,
    cookiePathOption = "/",
    cookieMaxAge = Some(sessionTimeoutInSeconds)
  )

}

object AuthConfigImpl {

  val CookieName = "BROCCOLI_SESS_ID"

}
