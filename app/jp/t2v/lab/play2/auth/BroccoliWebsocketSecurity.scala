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

  self: Controller with AuthConfig =>

  val securityService: SecurityService

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  // TODO do we keep the session alive somehow (e.g. by prolonging the timeout like in AsyncAuth)?
  def withSecurity[A](req: RequestHeader)
                     (f: (Option[AuthenticityToken], User, RequestHeader) => (Iteratee[A, _], Enumerator[A])): Future[Either[Result, (Iteratee[A, _], Enumerator[A])]] = {
    securityService.authMode match {
      case conf.AUTH_MODE_CONF =>
        val maybeToken = extractToken(req)
        val tokenString = maybeToken.getOrElse("<session ID missing>")
        val maybeUser = restoreUser(req, scala.concurrent.ExecutionContext.Implicits.global)
        maybeUser.recover {
          case exception =>
            Logger.info(s"Authenticating the following session failed (session probably outdated): $tokenString") // TODO log level
            (None, identity[Result] _) // don't follow IntelliJ's recommendation here!
        }.flatMap {
          // TODO do we need the updater here? can we even use cookies or should we authenticate for each new WS connection?
          case (Some(user), updater) =>
            Logger.info(s"Successfully authenticated session $tokenString of $user") // TODO log level
            Future.successful(Right(f(maybeToken, user, req)))
          case (None, _) =>
            Logger.info(s"Websocket to ${req.remoteAddress} not established because of missing authentication") // TODO log level
            authenticationFailed(req).map(result => Left(result))
        }
      case conf.AUTH_MODE_NONE =>
        Future.successful(Right(f(None, Anonymous, req)))
      case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = securityService.authMode match {
    case conf.AUTH_MODE_CONF => req.get (AuthKey).get
    case conf.AUTH_MODE_NONE => Anonymous.asInstanceOf[User]
    case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
  }

}
