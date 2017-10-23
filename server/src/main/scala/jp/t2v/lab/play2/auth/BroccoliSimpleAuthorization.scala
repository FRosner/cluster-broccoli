package jp.t2v.lab.play2.auth

import de.frosner.broccoli.auth.{Account, AuthMode}
import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.AuthConfigImpl
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.mvc.{Controller, RequestHeader, Result}

import scala.concurrent.Future

trait BroccoliSimpleAuthorization extends StackableController with AsyncAuth with AuthConfigImpl {

  self: Controller with AuthConfig =>

  val securityService: SecurityService

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(
      f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    securityService.authMode match {
      case AuthMode.Conf => {
        restoreUser recover {
          case _ => None -> identity[Result] _
        } flatMap {
          case (Some(u), cookieUpdater) => super.proceed(req.set(AuthKey, u))(f).map(cookieUpdater)
          case (None, _)                => authenticationFailed(req)
        }
      }
      case AuthMode.None => {
        super.proceed(req)(f)
      }
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = securityService.authMode match {
    case AuthMode.Conf => req.get(AuthKey).get
    case AuthMode.None => Account.anonymous
  }

  def getSessionId(request: RequestHeader): Option[AuthenticityToken] = extractToken(request)

}
