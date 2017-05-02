package jp.t2v.lab.play2.auth

import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.AuthConfigImpl
import de.frosner.broccoli.models.Anonymous
import de.frosner.broccoli.services.SecurityService
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.libs.iteratee.{Enumerator, Iteratee}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._

import scala.concurrent.Future

trait BroccoliWebsocketSecurity extends AsyncAuth with AuthConfigImpl with Logging {

  // TODO get rid of all this useless self inheritance implicit piece of ****?
  self: Controller with AuthConfig =>

  val securityService: SecurityService

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  def withSecurity[A](req: RequestHeader)(f: RequestHeader => (Iteratee[A, _], Enumerator[A])): Future[Either[Result, (Iteratee[A, _], Enumerator[A])]] = {
    securityService.authMode match {
      case conf.AUTH_MODE_CONF =>
        val maybeUser = restoreUser(req, scala.concurrent.ExecutionContext.Implicits.global)
        maybeUser.flatMap {
          case (Some(user), updater) => Future.successful(Right(f(req)))
          case (None, _) => authenticationFailed(req).map(result => Left(result))
        }
      case conf.AUTH_MODE_NONE =>
        Future.successful(Right(f(req)))
      case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = securityService.authMode match {
    case conf.AUTH_MODE_CONF => req.get (AuthKey).get
    case conf.AUTH_MODE_NONE => Anonymous.asInstanceOf[User]
    case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
  }

}
