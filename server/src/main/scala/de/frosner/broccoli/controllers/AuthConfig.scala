package de.frosner.broccoli.controllers

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.services.SecurityService
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.libs.json.JsString
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait AuthConfig {

  val securityService: SecurityService

  val playEnv: Environment

  val cacheApi: SyncCacheApi

  type Id = String

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = scala.reflect.classTag[Id]

  val sessionTimeoutInSeconds = securityService.sessionTimeoutInSeconds

  val cookieSecure = securityService.cookieSecure

  def resolveUser(id: Id): Future[Option[User]] =
    securityService.identityService.retrieve(LoginInfo(CredentialsProvider.ID, id))

  def loginSucceeded(request: RequestHeader): Future[Result] =
    Future.successful(Results.Ok(JsString("Login successful!"))) // the content is not used anyway as the controller replaces it

  def logoutSucceeded(request: RequestHeader): Future[Result] =
    Future.successful(Results.Ok(JsString("Logout successful!")))

  def authenticationFailed(request: RequestHeader): Future[Result] =
    Future.successful(Results.Forbidden("Authentication failed."))

  def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(
      implicit context: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden(
      s"Authorization failed: Your privileges (${user.role}) are not matching the required (${authority.getOrElse("None")})."))

  def authorize(user: User, authority: Authority): Future[Boolean] = Future.successful {
    user.role match {
      case Role.Administrator => true
      case Role.Operator      => authority == Role.Operator || authority == Role.User
      case Role.User          => authority == Role.User
    }
  }
}

object AuthConfig {

  val CookieName = "BROCCOLI_SESS_ID"

}
