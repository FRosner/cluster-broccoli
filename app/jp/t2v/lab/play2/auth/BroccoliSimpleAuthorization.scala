package jp.t2v.lab.play2.auth

import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.{Anonymous, AuthConfigImpl, UserAccount}
import de.frosner.broccoli.services.SecurityService
import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future

trait BroccoliSimpleAuthorization extends StackableController with AsyncAuth with AuthConfigImpl {

  self: Controller with AuthConfig =>

  val securityService: SecurityService

  private[auth] case object AuthKey extends RequestAttributeKey[User]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
     securityService.authMode match {
       case conf.AUTH_MODE_CONF => {
         restoreUser recover {
           case _ => None -> identity[Result] _
         } flatMap {
           case (Some(u), cookieUpdater) => super.proceed(req.set(AuthKey, u))(f).map(cookieUpdater)
           case (None, _) => authenticationFailed(req)
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
