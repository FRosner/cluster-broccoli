package de.frosner.broccoli.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.JsValue

// http://stackoverflow.com/questions/24576405/broadcasting-messages-in-play-framework-websockets
@Singleton
class WebSocketService @Inject() () extends Logging {

  @volatile
  private var connections: Map[String, Concurrent.Channel[Msg]] = Map.empty

  def newConnection(): (String, Enumerator[Msg]) = {
    val (enumerator, channel) = Concurrent.broadcast[Msg]
    val uuid = UUID.randomUUID().toString
    connections = connections.updated(uuid, channel)
    (uuid, enumerator)
  }

  def closeConnection(id: String): Unit = {
    connections.get(id) match {
      case Some(channel) =>
        channel.eofAndEnd()
        connections = connections - id
      case None => throw InvalidWebsocketConnectionException(id, connections.keys)
    }
  }

  def broadcast(msg: Msg): Unit = {
    connections.foreach {
      case (id, channel) =>
        Logger.debug(s"Broadcasting to $id: $msg")
        channel.push(msg)
    }
  }

  def send(id: String, msg: Msg): Unit = {
    connections.get(id) match {
      case Some(channel) => channel.push(msg)
      case None => throw InvalidWebsocketConnectionException(id, connections.keys)
    }
  }

//  def send(message: String) = channel.push(message)

}

object WebSocketService {

  type Msg = JsValue

}