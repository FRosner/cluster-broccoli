package de.frosner.broccoli.controllers

import jp.t2v.lab.play2.auth.{AuthConfig, CookieTokenAccessor}
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait AuthConfigImpl extends AuthConfig {

  type Id = String

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = scala.reflect.classTag[Id]

  val sessionTimeoutInSeconds: Int = 3600 // TODO make configurable

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Future(Some(Account("frank", "secret")))//Account.findById(id)

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok("loginSucceeded"))

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Ok("logoutSucceeded"))

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden("authenticationFailed"))

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Results.Forbidden("authorizationFailed"))
  }


  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    true
//    (user.role, authority) match {
//      case (Administrator, _)       => true
//      case (NormalUser, NormalUser) => true
//      case _                        => false
//    }
  }

  override lazy val tokenAccessor = new CookieTokenAccessor(
    // Set the secure flag only in production
    cookieSecureOption = play.api.Play.isProd(play.api.Play.current),
    cookieMaxAge       = Some(sessionTimeoutInSeconds)
  )

}
