package de.frosner.broccoli.services

import java.util.UUID
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.controllers._
import de.frosner.broccoli.models.{Account, Anonymous}
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.util.Logging
import play.api.Configuration
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

// http://stackoverflow.com/questions/24576405/broadcasting-messages-in-play-framework-websockets
@Singleton
class WebSocketService @Inject() (templateService: TemplateService,
                                  instanceService: InstanceService,
                                  aboutInfoService: AboutInfoService) extends Logging {

  private val scheduler = new ScheduledThreadPoolExecutor(1)
  private val task = new Runnable {
    def run() = {
      broadcast { user =>
        Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.AboutInfoMsg,
            AboutController.about(aboutInfoService, user)
          )
        )
      }

      broadcast { user =>
        Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.ListTemplatesMsg,
            TemplateController.list(templateService)
          )
        )
      }

      broadcast { user =>
        Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.ListInstancesMsg,
            InstanceController.list(None, user, instanceService)
          )
        )
      }
    }
  }
  private val scheduledTask = scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS)

  @volatile
  private var connections: Map[String, (Account, Concurrent.Channel[Msg])] = Map.empty

  def newConnection(user: Account): (String, Enumerator[Msg]) = {
    val id = UUID.randomUUID().toString
    (id, newConnection(id, user))
  }

  def newConnection(id: String, user: Account): Enumerator[Msg] = {
    val (enumerator, channel) = Concurrent.broadcast[Msg]
    if (connections.contains(id)) {
      val error = s"ID $id is already an open web socket connection"
      Logger.warn(error)
      throw new IllegalArgumentException(error)
    } else {
      val (enumerator, channel) = Concurrent.broadcast[Msg]
      connections = connections.updated(id, (user, channel))
      enumerator
    }
  }

  def closeConnection(id: String): Boolean = {
    connections.get(id).exists {
      case (user, channel) =>
        channel.eofAndEnd()
        connections = connections - id
        true
    }
  }

  def broadcast(msg: Account => Msg): Unit = {
    connections.foreach {
      case (id, (user, channel)) =>
        // TODO we're kinda swallowing the exceptions here. That's ok because currently we have no authorization on broadcasts
        val tryMsg = Try {
          val actualMsg = msg(user)
          channel.push(actualMsg)
          actualMsg
        }
        tryMsg match {
          case Success(actualMsg) => Logger.debug(s"Broadcasting to $id: $msg")
          case Failure(throwable) => Logger.warn(s"Broadcasting to $id failed: $throwable")
        }
    }
  }

  def send(id: String, msg: Msg): Unit = {
    connections.get(id) match {
      case Some((user, channel)) => channel.push(msg)
      case None => throw InvalidWebsocketConnectionException(id, connections.keys)
    }
  }

//  def send(message: String) = channel.push(message)

}

object WebSocketService {

  type Msg = JsValue

}