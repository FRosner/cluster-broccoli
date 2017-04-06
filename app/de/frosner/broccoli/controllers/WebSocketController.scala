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
  // TODO authorization (how to manage that clients only receive updates they are allowed to)

  def socket = WebSocket.using[String] { request =>
    Logger.info(s"Opening websocket: $request")
    val in = Iteratee.foreach[String] {
          // TODO call the controller methods
      msg =>
          println(s"Received msg: $msg")
//        webSocketService.send(msg)
    }.map { _ =>
      println(s"WS closed: $request")
    }

    // TODO send request to set all instances and templates initially, then the webSocketService.channel will be used for subsequent updates

    (in, webSocketService.out)
  }

}