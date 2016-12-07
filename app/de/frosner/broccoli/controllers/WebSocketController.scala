package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services.WebSocketService
import de.frosner.broccoli.util.Logging
import play.api.mvc._
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global

class WebSocketController @Inject() (webSocketService: WebSocketService) extends Controller with Logging {

  // TODO close on logout
  // TODO authentication with WebSocket.tryAccept
  // TODO authorization

  def socket = WebSocket.using[String] { request =>
    Logger.info(s"Opening websocket: $request")
    val in = Iteratee.foreach[String] {
      msg =>
        webSocketService.channel push(msg)
    }

    (in, webSocketService.out)
  }

}