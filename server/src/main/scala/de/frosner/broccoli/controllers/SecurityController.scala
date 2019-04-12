package de.frosner.broccoli.controllers

import javax.inject.Inject
import cats.data.{EitherT, OptionT}
import cats.instances.future._
import cats.syntax.either._
import com.mohiva.play.silhouette.api.{LoginEvent, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{BroccoliSimpleAuthorization, DefaultEnv}
import de.frosner.broccoli.services.{SecurityService, WebSocketService}
import play.api.{Environment, Logger}
import play.api.cache.SyncCacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.{JsString, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class SecurityController @Inject()(
    override val securityService: SecurityService,
    override val cacheApi: SyncCacheApi,
    override val playEnv: Environment,
    override val controllerComponents: ControllerComponents,
    override val silhouette: Silhouette[DefaultEnv],
    webSocketService: WebSocketService,
    credentialsProvider: CredentialsProvider,
    override val executionContext: ExecutionContext
) extends BaseController
    with BroccoliSimpleAuthorization {

  private val log = Logger(getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  // https://www.playframework.com/documentation/2.5.x/ScalaForms
  val loginForm = Form {
    mapping(
      SecurityController.UsernameFormKey -> text,
      SecurityController.PasswordFormKey -> text
    )(Credentials.apply)(Credentials.unapply)
  }

  def login: Action[AnyContent] = Action.async { implicit request =>
    getSessionId(request).map { maybeId =>
      maybeId.map(id => (id, webSocketService.closeConnections(id))) match {
        case Some((id, true))  => log.info(s"Removing websocket connection of $id due to another login")
        case Some((id, false)) => log.info(s"No connection found for session $id.")
        case _                 =>
      }
    }
    (for {
      credentials <- EitherT.fromEither[Future](
        loginForm.bindFromRequest().fold(Function.const(Results.BadRequest.asLeft), _.asRight))
      login <- OptionT(securityService.authenticate(credentials)).toRight(Results.Unauthorized)
      user <- OptionT(resolveUser(login.providerKey)).toRight(Results.Unauthorized)
      authenticator <- OptionT(silhouette.env.authenticatorService.create(login).map(Option(_)))
        .toRight(Results.Unauthorized)
    } yield {
      (user, authenticator)
    }).semiflatMap {
      case (user, authenticator) =>
        val userResult = Results.Ok(Json.toJson(user))
        silhouette.env.eventBus.publish(LoginEvent(user, request))
        silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
          silhouette.env.authenticatorService.embed(v, userResult)
        }
    }.merge
  }

  def logout = silhouette.SecuredAction.async(parse.empty) { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request))
    // Get the sessionId before removing it
    getSessionId(request)
      .flatMap { maybeSessionId =>
        env.authenticatorService
          .discard(request.authenticator, Ok(JsString("Logout successful!")))
          .andThen {
            case Failure(t) =>
              log.error("Failed to logout", t)
            case Success(r) =>
              log.info("Successfully logged out")
              maybeSessionId.map(id => (id, webSocketService.closeConnections(id))) match {
                case Some((id, true))  => log.info(s"Removing websocket connection of $id due to logout")
                case Some((id, false)) => log.info(s"There was no websocket connection for session $id")
                case None              => log.info(s"No session available to logout from")
              }
          }
      }
  }

  def verify = Action.async(parse.empty) { implicit request =>
    loggedIn { implicit user =>
      Ok(user.name)
    }
  }
}

object SecurityController {

  val UsernameFormKey = "username"
  val PasswordFormKey = "password"

}
