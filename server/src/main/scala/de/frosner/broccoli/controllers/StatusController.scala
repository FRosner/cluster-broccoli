package de.frosner.broccoli.controllers

import play.api.mvc.{Action, AnyContent, Controller, Results}

class StatusController extends Controller {

  def status: Action[AnyContent] = Action(Results.Ok)

}
