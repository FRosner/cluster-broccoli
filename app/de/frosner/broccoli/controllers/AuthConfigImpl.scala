package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.{AuthConfig, CookieTokenAccessor}
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait AuthConfigImpl extends AuthConfig with Logging {

  val configuration: Configuration

  type Id = String

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = scala.reflect.classTag[Id]

  // TODO check if setting it wrong will exit also in production
  private lazy val sessionTimeOutString: Option[String] = configuration.getString(conf.AUTH_SESSION_TIMEOUT_KEY)
  private lazy val sessionTimeOutTry: Try[Int] = sessionTimeOutString match {
    case Some(string) => Try(string.toInt).flatMap {
      int => if (int >= 1) Success(int) else Failure(new Exception())
    }
    case None => Success(conf.AUTH_SESSION_TIMEOUT_DEFAULT)
  }
  if (sessionTimeOutTry.isFailure) {
    Logger.error(s"Invalid ${conf.AUTH_SESSION_TIMEOUT_DEFAULT} specified: '${sessionTimeOutString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  lazy val sessionTimeoutInSeconds = sessionTimeOutTry.get
  Logger.info(s"${conf.AUTH_SESSION_TIMEOUT_KEY}=$sessionTimeoutInSeconds")

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
