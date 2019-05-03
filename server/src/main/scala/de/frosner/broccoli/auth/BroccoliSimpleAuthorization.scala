package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.HandlerResult
import de.frosner.broccoli.auth.Account.anonymous
import de.frosner.broccoli.controllers.AuthConfig
import de.frosner.broccoli.services.SecurityService
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait BroccoliSimpleAuthorization extends AuthConfig with BroccoliSecurity {

  self: BaseController with AuthConfig =>

  val securityService: SecurityService

  implicit val executionContext: ExecutionContext

  implicit def loggedIn[B](f: DefaultEnv#I => Result)(implicit request: Request[B]): Future[Result] =
    securityService.authMode match {
      case AuthMode.Conf =>
        silhouette
          .UserAwareRequestHandler(request) { implicit userAwareRequest =>
            Future.successful(HandlerResult(Ok, userAwareRequest.identity))
          }
          .map {
            case HandlerResult(r, Some(user)) => f(user)
            case HandlerResult(r, None)       => Results.Forbidden("Authentication failed.")
          }
      case AuthMode.None => Future.successful(f(anonymous))
    }

  implicit def asyncLoggedIn[B](f: DefaultEnv#I => Future[Result])(implicit request: Request[B]): Future[Result] =
    securityService.authMode match {
      case AuthMode.Conf =>
        silhouette
          .UserAwareRequestHandler(request) { userAwareRequest =>
            Future.successful(HandlerResult(Ok, userAwareRequest.identity))
          }
          .map {
            case HandlerResult(r, Some(user)) => f(user)
            case HandlerResult(r, None)       => Future.successful(Results.Forbidden("Authentication failed."))
          }
          .flatten
      case AuthMode.None => f(anonymous)
    }

  def getSessionId[A](request: Request[A]): Future[Option[AuthenticityToken]] =
    silhouette.env.authenticatorService.retrieve(request).map(_.map(_.id))

}
