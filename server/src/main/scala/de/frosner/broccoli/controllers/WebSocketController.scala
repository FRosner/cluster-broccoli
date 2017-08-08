package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.OutgoingWsMessage._
import de.frosner.broccoli.services._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.util.Logging
import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.models.Instance.instanceApiWrites
import de.frosner.broccoli.models._
import jp.t2v.lab.play2.auth.{BroccoliSimpleAuthorization, BroccoliWebsocketSecurity}
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

case class WebSocketController @Inject()(webSocketService: WebSocketService,
                                         templateService: TemplateService,
                                         instanceService: InstanceService,
                                         aboutService: AboutInfoService,
                                         override val securityService: SecurityService)
    extends Controller
    with Logging
    with BroccoliWebsocketSecurity {

  def requestToSocket(request: RequestHeader): Future[Either[Result, (Iteratee[Msg, _], Enumerator[Msg])]] =
    withSecurity(request) { (maybeToken, user, request) =>
      val (connectionId, connectionEnumerator) = maybeToken match {
        case Some(token) =>
          (token, webSocketService.newConnection(token, user)) // auth is enabled and we can use the session ID
        case None => webSocketService.newConnection(user) // no session ID available so we generate one
      }
      val connectionLogString = s"$connectionId by $user from ${request.remoteAddress} at $request"
      Logger.info(s"New connection $connectionLogString")

      // TODO receive string and try json decoding here because I can handle the error better
      val in = Iteratee
        .foreach[Msg] { incomingMessage =>
          val outgoingMessage = Json
            .fromJson[IncomingWsMessage](incomingMessage)
            .map {
              case IncomingWsMessage.AddInstance(instanceCreation) =>
                InstanceController
                  .create(instanceCreation, user, instanceService)
                  .fold(AddInstanceError, AddInstanceSuccess)
              case IncomingWsMessage.DeleteInstance(instanceId) =>
                InstanceController
                  .delete(instanceId, user, instanceService)
                  .fold(DeleteInstanceError, DeleteInstanceSuccess)
              case IncomingWsMessage.UpdateInstance(instanceUpdate: InstanceUpdate) =>
                OutgoingWsMessage.fromResult(
                  InstanceController.update(instanceUpdate.instanceId.get, instanceUpdate, user, instanceService))
            }
            .recoverTotal { error =>
              Logger.warn(s"Can't parse a message from $connectionId: $error")
              OutgoingWsMessage.Error(s"Failed to parse message message: $error")
            }
          webSocketService.send(connectionId, Json.toJson(outgoingMessage))
        }
        .map { _ =>
          webSocketService.closeConnections(connectionId)
          Logger.info(s"Closed connection $connectionLogString")
        }

      val aboutEnumerator =
        Enumerator[Msg](Json.toJson(OutgoingWsMessage.AboutInfoMsg(AboutController.about(aboutService, user))))

      val templateEnumerator = Enumerator[Msg](
        Json.toJson(
          OutgoingWsMessage.ListTemplates(TemplateController.list(templateService))
        ))

      val instanceEnumerator = Enumerator[Msg](
        Json.toJson(OutgoingWsMessage.ListInstances(InstanceController.list(None, user, instanceService))))
      (in, aboutEnumerator.andThen(templateEnumerator).andThen(instanceEnumerator).andThen(connectionEnumerator))
    }

  def socket: WebSocket = WebSocket.tryAccept[Msg](requestToSocket)

}
