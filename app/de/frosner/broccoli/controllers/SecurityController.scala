package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller, Results}

import scala.concurrent.{ExecutionContext, Future}

class SecurityController extends Controller with Logging with LoginLogout with AuthElement with AuthConfigImpl {

  import scala.concurrent.ExecutionContext.Implicits.global

  // https://www.playframework.com/documentation/2.5.x/ScalaForms
  val loginForm = Form {
    mapping(
      "username" -> text,
      "password" -> text
    )(Account.apply)(Account.unapply)
  }

  def login = Action.async { implicit request =>
    Logger.info("login: " + request.body.toString)
    loginForm.bindFromRequest().fold(
      formWithErrors => Future.successful(Results.BadRequest),
      account => gotoLoginSucceeded(account.name)
    )
  }

  def logout = Action.async { implicit request =>
    Logger.info("logout: " + request.body.toString)
    gotoLogoutSucceeded
  }

  def verify = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val user = loggedIn
    val title = "message main"
    Ok(user.name)
  }

}
