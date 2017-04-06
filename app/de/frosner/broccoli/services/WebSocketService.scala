package de.frosner.broccoli.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.api.libs.iteratee.{Concurrent, Enumerator}

// http://stackoverflow.com/questions/24576405/broadcasting-messages-in-play-framework-websockets
@Singleton
class WebSocketService @Inject() () extends Logging {

  @volatile
  private var connections: Map[String, Concurrent.Channel[String]] = Map.empty

  def newConnection(): (String, Enumerator[String]) = {
    val (enumerator, channel) = Concurrent.broadcast[String]
    val uuid = UUID.randomUUID().toString
    connections = connections.updated(uuid, channel)
    (uuid, enumerator)
  }

  def closeConnection(id: String): Unit = {
    connections.get(id) match {
      case Some(channel) =>
        channel.eofAndEnd()
        connections = connections - id
      case None => throw new IllegalArgumentException(s"Connection $id is not in the connection pool: " +
        s"${connections.keys.mkString(", ")}")
    }
  }

//  def send(message: String) = channel.push(message)

}
