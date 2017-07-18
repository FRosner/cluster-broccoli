package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{UserAccount, UserCredentials}
import de.frosner.broccoli.services.{SecurityService, WebSocketService}
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.{BroccoliSimpleAuthorization, LoginLogout}
import play.api.Configuration
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{Action, Controller, Results}

import scala.concurrent.{ExecutionContext, Future}

case class SecurityController @Inject()(override val securityService: SecurityService,
                                        webSocketService: WebSocketService)
    extends Controller
    with Logging
    with LoginLogout
    with BroccoliSimpleAuthorization {

  import scala.concurrent.ExecutionContext.Implicits.global

  // https://www.playframework.com/documentation/2.5.x/ScalaForms
  val loginForm = Form {
    mapping(
      SecurityController.UsernameFormKey -> text,
      SecurityController.PasswordFormKey -> text
    )(UserCredentials.apply)(UserCredentials.unapply)
  }

  def login = Action.async { implicit request =>
    getSessionId(request).map(id => (id, webSocketService.closeConnections(id))) match {
      case Some((id, true)) => Logger.info(s"Removing websocket connection of $id due to another login")
      case _                =>
    }
    loginForm
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Results.BadRequest),
        account => {
          if (securityService.isAllowedToAuthenticate(account)) {
            Logger.info(s"Login successful for user '${account.name}'.")
            val eventuallyLoginSucceededResponse = gotoLoginSucceeded(account.name)
            eventuallyLoginSucceededResponse.flatMap {
              result =>
                val user = resolveUser(account.name)
                user.map {
                  maybeUser =>
                    val actualUser = maybeUser.get
                    val userResult = Results.Ok(
                      JsObject(
                        Map(
                          "name" -> JsString(actualUser.name),
                          "role" -> JsString(actualUser.role.toString),
                          "instanceRegex" -> JsString(actualUser.instanceRegex)
                        ))
                    )
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
                }
            }
          } else {
            Logger.info(s"Login failed for user '${account.name}'.")
            Future.successful(Results.Unauthorized)
          }
        }
      )
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.andThen {
      case tryResult =>
        getSessionId(request).map(id => (id, webSocketService.closeConnections(id))) match {
          case Some((id, true))  => Logger.info(s"Removing websocket connection of $id due to logout")
          case Some((id, false)) => Logger.info(s"There was no websocket connection for session $id")
          case None              => Logger.info(s"No session available to logout from")
        }
    }
  }

  def verify = StackAction { implicit request =>
    val user = loggedIn
    Ok(user.name)
  }

}

object SecurityController {

  val UsernameFormKey = "username"
  val PasswordFormKey = "password"

}
