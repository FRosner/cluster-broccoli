package de.frosner.broccoli.controllers

import javax.inject.Inject

import play.api.mvc.{Action, AnyContent, Controller, Results}

class StatusController extends Controller {

  def status: Action[AnyContent] = Action(Results.Ok)

}
