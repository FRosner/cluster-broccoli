package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.models.{UserAccount, UserCredentials}
import de.frosner.broccoli.services.SecurityService
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.{BroccoliSimpleAuthorization, LoginLogout}
import play.api.Configuration
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller, Results}

import scala.concurrent.{ExecutionContext, Future}

case class SecurityController @Inject() (override val securityService: SecurityService)
  extends Controller with Logging with LoginLogout with BroccoliSimpleAuthorization {

  import scala.concurrent.ExecutionContext.Implicits.global

  // https://www.playframework.com/documentation/2.5.x/ScalaForms
  val loginForm = Form {
    mapping(
      SecurityController.UsernameFormKey -> text,
      SecurityController.PasswordFormKey -> text
    )(UserCredentials.apply)(UserCredentials.unapply)
  }

  def login = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      formWithErrors => Future.successful(Results.BadRequest),
      account => {
        if (securityService.isAllowedToAuthenticate(account)) {
          Logger.info(s"Login successful for user '${account.name}'.")
          gotoLoginSucceeded(account.name)
        } else {
          Logger.info(s"Login failed for user '${account.name}'.")
          Future.successful(Results.Unauthorized)
        }
      }
    )
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def verify = StackAction { implicit request =>
    val user = loggedIn
    val title = "message main"
    Ok(user.name)
  }

}

object SecurityController {

  val UsernameFormKey = "username"
  val PasswordFormKey = "password"

}
