package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import de.frosner.broccoli.conf
import de.frosner.broccoli.services._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.controllers.OutgoingWsMessageType._
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

case class WebSocketController @Inject()
  ( webSocketService: WebSocketService
  , templateService: TemplateService
  , instanceService: InstanceService
  , aboutService: AboutInfoService
  , override val securityService: SecurityService
  ) extends Controller with Logging with BroccoliWebsocketSecurity {

  def requestToSocket(request: RequestHeader): Future[Either[Result, (Iteratee[Msg, _], Enumerator[Msg])]] = {
      withSecurity(request) { (maybeToken, user, request) =>
        val (connectionId, connectionEnumerator) = maybeToken match {
          case Some(token) => (token, webSocketService.newConnection(token, user)) // auth is enabled and we can use the session ID
          case None => webSocketService.newConnection(user) // no session ID available so we generate one
        }
        val connectionLogString = s"$connectionId by $user from ${request.remoteAddress} at $request"
        Logger.info(s"New connection $connectionLogString")

        // TODO receive string and try json decoding here because I can handle the error better
        val in = Iteratee.foreach[Msg] {
          jsMsg =>
            val msg = Json.fromJson(jsMsg)(IncomingWsMessage.incomingWsMessageReads)
            val result = msg.map {
              case IncomingWsMessage(IncomingWsMessageType.AddInstance, instanceCreation: InstanceCreation) =>
                InstanceController.create(instanceCreation, user, instanceService) match {
                  case success: InstanceCreationSuccess =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceCreationSuccessMsg, success)
                  case failure: InstanceCreationFailure =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceCreationFailureMsg, failure)
                }
              case IncomingWsMessage(IncomingWsMessageType.DeleteInstance, instanceId: String) =>
                InstanceController.delete(instanceId, user, instanceService) match {
                  case success: InstanceDeletionSuccess =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceDeletionSuccessMsg, success)
                  case failure: InstanceDeletionFailure =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceDeletionFailureMsg, failure)
                }
              case IncomingWsMessage(IncomingWsMessageType.UpdateInstance, instanceUpdate: InstanceUpdate) =>
                InstanceController.update(instanceUpdate.instanceId.get, instanceUpdate, user, instanceService) match {
                  case success: InstanceUpdateSuccess =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceUpdateSuccessMsg, success)
                  case failure: InstanceUpdateFailure =>
                    OutgoingWsMessage(OutgoingWsMessageType.InstanceUpdateFailureMsg, failure)
                }
            }.getOrElse {
              Logger.warn(s"Can't parse a message from $connectionId: $msg")
              OutgoingWsMessage(OutgoingWsMessageType.ErrorMsg, "Received unparsable message")
            }
            webSocketService.send(connectionId, Json.toJson(result))
        }.map { _ =>
          webSocketService.closeConnection(connectionId)
          Logger.info(s"Closed connection $connectionLogString")
        }

        val aboutEnumerator = Enumerator[Msg](Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.AboutInfoMsg,
            AboutController.about(aboutService, user)
          )
        ))

        val templateEnumerator = Enumerator[Msg](Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.ListTemplatesMsg,
            TemplateController.list(templateService)
          )
        ))

        val instanceEnumerator = Enumerator[Msg](Json.toJson(
          OutgoingWsMessage(
            OutgoingWsMessageType.ListInstancesMsg,
            InstanceController.list(None, user, instanceService)
          )
        ))
        (in, aboutEnumerator.andThen(templateEnumerator).andThen(instanceEnumerator).andThen(connectionEnumerator))
      }
  }

  def socket: WebSocket[Msg, Msg] = WebSocket.tryAccept[Msg](requestToSocket)

}

