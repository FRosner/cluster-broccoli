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

    // TODO create web socket with id and store it here so we can delete it later
    val (connectionId, enumerator) = webSocketService.newConnection()
    val connectionLogString = s"$connectionId ($request) from ${request.remoteAddress}"
    Logger.info(s"New connection $connectionLogString")

    val in = Iteratee.foreach[String] {
        // TODO call the controller methods
      msg =>
        println(s"$connectionId Received msg: $msg")
    }.map { _ =>
      webSocketService.closeConnection(connectionId)
      Logger.info(s"Closed connection $connectionLogString")
    }

    // TODO send request to set all instances and templates initially, then the webSocketService.channel will be used for subsequent updates
    (in, enumerator)
  }

}