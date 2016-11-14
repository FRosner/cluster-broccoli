package jp.t2v.lab.play2.auth

import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.Anonymous
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future

trait BroccoliRoleAuthorization extends StackableController with AsyncAuth {

  self: Controller with AuthConfig =>

  val securityService: SecurityService

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
    securityService.authMode match {
      case conf.AUTH_MODE_CONF => {
        req.get(AuthorityKey) map { authority =>
          authorized(authority) flatMap {
            case Right((user, resultUpdater)) => super.proceed(req.set(AuthKey, user))(f).map(resultUpdater)
            case Left(result)                 => Future.successful(result)
          }
        } getOrElse {
          restoreUser collect {
            case (Some(user), _) => user
          } flatMap {
            authorizationFailed(req, _, None)
          } recoverWith {
            case _ => authenticationFailed(req)
          }
        }
      }
      case conf.AUTH_MODE_NONE => {
        super.proceed(req)(f)
      }
      case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
    }
  }

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = securityService.authMode match {
    case conf.AUTH_MODE_CONF => req.get (AuthKey).get
    case conf.AUTH_MODE_NONE => Anonymous.asInstanceOf[User]
    case other => throw new IllegalStateException(s"Unrecognized auth mode: ${securityService.authMode}")
  }

}
