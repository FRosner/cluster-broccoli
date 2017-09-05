package de.frosner.broccoli.controllers

import javax.inject.Inject

import cats.data.{EitherT, OptionT}
import cats.instances.future._
import cats.syntax.either._
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.services.{SecurityService, WebSocketService}
import jp.t2v.lab.play2.auth.{BroccoliSimpleAuthorization, LoginLogout}
import play.api.{Environment, Logger}
import play.api.cache.CacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller, Results}

import scala.concurrent.Future

case class SecurityController @Inject()(
    override val securityService: SecurityService,
    override val cacheApi: CacheApi,
    override val playEnv: Environment,
    webSocketService: WebSocketService
) extends Controller
    with LoginLogout
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
    getSessionId(request).map(id => (id, webSocketService.closeConnections(id))) match {
      case Some((id, true)) => log.info(s"Removing websocket connection of $id due to another login")
      case _                =>
    }
    (for {
      credentials <- EitherT.fromEither[Future](
        loginForm.bindFromRequest().fold(Function.const(Results.BadRequest.asLeft), _.asRight))
      _ <- EitherT
        .pure(securityService.isAllowedToAuthenticate(credentials))
        .ensure {
          log.info(s"Login failed for user '${credentials.identifier}'.")
          Results.Unauthorized
        }(identity)
      _ = log.info(s"Login successful for user '${credentials.identifier}'.")
      result <- EitherT.right(gotoLoginSucceeded(credentials.identifier))
      user <- OptionT(resolveUser(credentials.identifier)).toRight(Results.Unauthorized)
    } yield {
      val userResult = Results.Ok(Json.toJson(user))
      result.copy(
        header = result.header.copy(
          headers = userResult.header.headers
            .get("Content-Type")
            .map { contentType =>
              result.header.headers.updated("Content-Type", contentType)
            }
            .getOrElse(result.header.headers)
        ),
        body = userResult.body
      )
    }).merge
  }

  def logout = Action.async(parse.empty) { implicit request =>
    gotoLogoutSucceeded.andThen {
      case tryResult =>
        getSessionId(request).map(id => (id, webSocketService.closeConnections(id))) match {
          case Some((id, true))  => log.info(s"Removing websocket connection of $id due to logout")
          case Some((id, false)) => log.info(s"There was no websocket connection for session $id")
          case None              => log.info(s"No session available to logout from")
        }
    }
  }

  def verify = StackAction(parse.empty) { implicit request =>
    Ok(loggedIn.name)
  }

}

object SecurityController {

  val UsernameFormKey = "username"
  val PasswordFormKey = "password"

}
