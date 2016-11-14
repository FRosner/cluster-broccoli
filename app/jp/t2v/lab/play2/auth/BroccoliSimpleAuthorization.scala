package jp.t2v.lab.play2.auth

import jp.t2v.lab.play2.stackc.{RequestAttributeKey, RequestWithAttributes, StackableController}
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future

trait BroccoliSimpleAuthorization extends StackableController with AsyncAuth {
  self: Controller with AuthConfig =>

  private[auth] case object AuthKey extends RequestAttributeKey[User]
  case object AuthorityKey extends RequestAttributeKey[Authority]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    implicit val (r, ctx) = (req, StackActionExecutionContext(req))
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

  implicit def loggedIn(implicit req: RequestWithAttributes[_]): User = req.get(AuthKey).get

}
