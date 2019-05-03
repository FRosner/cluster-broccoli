package de.frosner.broccoli.auth

import akka.stream.scaladsl.Flow
import com.mohiva.play.silhouette.api.HandlerResult
import de.frosner.broccoli.auth.Account.anonymous
import de.frosner.broccoli.controllers.AuthConfig
import de.frosner.broccoli.services.SecurityService
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait BroccoliWebsocketSecurity extends AuthConfig with BroccoliSecurity {

  self: BaseController with AuthConfig =>

  protected def log: Logger

  val securityService: SecurityService

  implicit val executionContext: ExecutionContext

  def withSecurity[A](req: RequestHeader)(
      f: (Option[AuthenticityToken], User, RequestHeader) => Flow[A, A, _]): Future[Either[Result, Flow[A, A, _]]] =
    securityService.authMode match {
      case AuthMode.Conf =>
        implicit val request: Request[AnyContentAsEmpty.type] = Request(req, AnyContentAsEmpty)
        silhouette
          .SecuredRequestHandler { securedRequest =>
            Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
          }
          .map {
            case HandlerResult(r, Some(user)) =>
              env.authenticatorService.retrieve(request).map { maybeAuthenticator =>
                val r1 =
                  f(
                    maybeAuthenticator.map(_.id),
                    user,
                    req
                  )
                Right[Result, Flow[A, A, _]](r1)
              }
            case HandlerResult(r, None) =>
              Future.successful(Left[Result, Flow[A, A, _]](Forbidden))
          }
          .flatten
      case AuthMode.None =>
        Future.successful(Right(f(None, anonymous, req)))
    }

  implicit def loggedIn[B](f: DefaultEnv#I => Result)(implicit request: Request[B]): Future[Result] =
    securityService.authMode match {
      case AuthMode.Conf =>
        silhouette
          .UserAwareRequestHandler(request) { userAwareRequest =>
            Future.successful(HandlerResult(Ok, userAwareRequest.identity))
          }
          .map {
            case HandlerResult(r, Some(user)) => f(user)
            case HandlerResult(r, None)       => Unauthorized
          }
      case AuthMode.None => Future.successful(f(anonymous))
    }

}
