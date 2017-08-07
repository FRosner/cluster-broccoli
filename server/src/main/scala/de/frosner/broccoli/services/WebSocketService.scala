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
class WebSocketService @Inject()(templateService: TemplateService,
                                 instanceService: InstanceService,
                                 aboutInfoService: AboutInfoService)
    extends Logging {

  private val scheduler = new ScheduledThreadPoolExecutor(1)
  private val task = new Runnable {
    def run() = {
      broadcast { user =>
        Json.toJson(OutgoingWsMessage.AboutInfoMsg(AboutController.about(aboutInfoService, user)))
      }
      broadcast { _ =>
        Json.toJson(OutgoingWsMessage.ListTemplates(TemplateController.list(templateService)))
      }
      broadcast { user =>
        Json.toJson(OutgoingWsMessage.ListInstances(InstanceController.list(None, user, instanceService)))
      }
    }
  }
  private val scheduledTask = scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS)

  @volatile
  private var connections: Map[String, (Account, Set[Concurrent.Channel[Msg]])] = Map.empty

  def newConnection(user: Account): (String, Enumerator[Msg]) = {
    val id = UUID.randomUUID().toString
    (id, newConnection(id, user))
  }

  def newConnection(id: String, user: Account): Enumerator[Msg] = {
    if (connections.contains(id)) {
      Logger.info(
        s"ID $id (${user.name}) already has an open web socket connection. Probably he/she has two tabs open?")
    }
    val (enumerator, channel) = Concurrent.broadcast[Msg]
    val maybeExistingChannels = connections.get(id)
    val existingChannels = maybeExistingChannels
      .map {
        case (u, c) => c
      }
      .getOrElse(Set.empty)
    connections = connections.updated(id, (user, existingChannels + channel))
    enumerator
  }

  def closeConnections(id: String): Boolean =
    connections.get(id).exists {
      case (user, channels) =>
        channels.foreach(_.eofAndEnd())
        connections = connections - id
        true
    }

  def broadcast(msg: Account => Msg): Unit =
    connections.foreach {
      case (id, (user, channels)) =>
        // TODO we're kinda swallowing the exceptions here. That's ok because currently we have no authorization on broadcasts
        val tryMsg = Try {
          val actualMsg = msg(user)
          channels.foreach(_.push(actualMsg))
          actualMsg
        }
        tryMsg match {
          case Success(actualMsg) => Logger.debug(s"Broadcasting to $id: $msg")
          case Failure(throwable) => Logger.warn(s"Broadcasting to $id failed: $throwable")
        }
    }

  def send(id: String, msg: Msg): Unit =
    connections.get(id) match {
      case Some((user, channels)) => channels.foreach(_.push(msg))
      case None                   => throw InvalidWebsocketConnectionException(id, connections.keys)
    }

}

object WebSocketService {

  type Msg = JsValue

}
