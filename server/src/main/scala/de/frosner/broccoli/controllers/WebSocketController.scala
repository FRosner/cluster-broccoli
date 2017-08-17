package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import de.frosner.broccoli.websocket.{IncomingMessage, OutgoingMessage, WebSocketMessageHandler}
import jp.t2v.lab.play2.auth.BroccoliWebsocketSecurity
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

case class WebSocketController @Inject()(webSocketService: WebSocketService,
                                         templateService: TemplateService,
                                         instanceService: InstanceService,
                                         aboutService: AboutInfoService,
                                         messageHandler: WebSocketMessageHandler,
                                         override val securityService: SecurityService)
    extends Controller
    with BroccoliWebsocketSecurity {

  protected val log = Logger(getClass)

  def requestToSocket(request: RequestHeader): Future[Either[Result, (Iteratee[Msg, _], Enumerator[Msg])]] =
    withSecurity(request) { (maybeToken, user, request) =>
      val (connectionId, connectionEnumerator) = maybeToken match {
        case Some(token) =>
          (token, webSocketService.newConnection(token, user)) // auth is enabled and we can use the session ID
        case None => webSocketService.newConnection(user) // no session ID available so we generate one
      }
      val connectionLogString = s"$connectionId by $user from ${request.remoteAddress} at $request"
      log.info(s"New connection $connectionLogString")

      // TODO receive string and try json decoding here because I can handle the error better
      val in = Enumeratee.mapM[Msg] { incomingMessage =>
        Json
          .fromJson[IncomingMessage](incomingMessage)
          .map(messageHandler.processMessage(user))
          .recoverTotal { error =>
            log.warn(s"Can't parse a message from $connectionId: $error")
            Future.successful(OutgoingMessage.Error(s"Failed to parse message message: $error"))
          }
      } transform Iteratee
        .foreach[OutgoingMessage](msg => webSocketService.send(connectionId, Json.toJson(msg)))
        .map { _ =>
          webSocketService.closeConnections(connectionId)
          log.info(s"Closed connection $connectionLogString")
        }

      val aboutEnumerator =
        Enumerator[Msg](Json.toJson(OutgoingMessage.AboutInfoMsg(AboutController.about(aboutService, user))))

      val templateEnumerator = Enumerator[Msg](
        Json.toJson(
          OutgoingMessage.ListTemplates(TemplateController.list(templateService))
        ))

      val instanceEnumerator = Enumerator[Msg](
        Json.toJson(OutgoingMessage.ListInstances(InstanceController.list(None, user, instanceService))))
      (in, aboutEnumerator.andThen(templateEnumerator).andThen(instanceEnumerator).andThen(connectionEnumerator))
    }

  def socket: WebSocket = WebSocket.tryAccept[Msg](requestToSocket)

}
