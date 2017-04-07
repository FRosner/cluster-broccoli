package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services.{TemplateService, WebSocketService}
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.util.Logging
import de.frosner.broccoli.models.Template.templateApiWrites
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json.{JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global

class WebSocketController @Inject() ( webSocketService: WebSocketService
                                    , templateService: TemplateService
                                    ) extends Controller with Logging {

  // TODO close on logout
  // TODO authentication with WebSocket.tryAccept
  // TODO authorization (how to manage that clients only receive updates they are allowed to)

  def socket = WebSocket.using[Msg] { request =>
    val (connectionId, enumerator) = webSocketService.newConnection()
    val connectionLogString = s"$connectionId ($request) from ${request.remoteAddress}"
    Logger.info(s"New connection $connectionLogString")
    // webSocketService.send(connectionId, "New templates and instances gogo.") only works after the enumerator and in has been passed
    // so when / where do we send the initial update?


    val in = Iteratee.foreach[Msg] {
        // TODO call the controller methods
      msg =>
        Logger.debug(s"Received message through $connectionId: $msg")
    }.map { _ =>
      webSocketService.closeConnection(connectionId)
      Logger.info(s"Closed connection $connectionLogString")
    }

    // TODO send request to set all instances and templates initially, then the webSocketService.channel will be used for subsequent updates
    val initialEnumerator = Enumerator[Msg](Json.toJson(templateService.getTemplates))
    (in, initialEnumerator.andThen(enumerator))
  }

}