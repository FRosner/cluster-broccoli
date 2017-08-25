package de.frosner.broccoli.controllers

import javax.inject.Inject

import cats.data.OptionT
import cats.instances.future._
import com.mohiva.play.silhouette.api.util.Credentials
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
    loginForm
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Results.BadRequest),
        credentials => {
          (for {
            loginInfo <- securityService.authenticate(credentials)
            result <- OptionT.liftF(gotoLoginSucceeded(loginInfo.providerKey))
            user <- OptionT(resolveUser(loginInfo.providerKey))
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
          }).getOrElse {
            log.info(s"Login failed for user '${credentials.identifier}'.")
            Results.Unauthorized
          }
        }
      )
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
