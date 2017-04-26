package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.conf
import de.frosner.broccoli.services._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.controllers.OutgoingWsMessageType._
import de.frosner.broccoli.util.Logging
import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.models.Instance.instanceApiWrites
import de.frosner.broccoli.models._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class WebSocketController @Inject() ( webSocketService: WebSocketService
                                    , templateService: TemplateService
                                    , instanceService: InstanceService
                                    , aboutService: AboutInfoService
                                    , securityService: SecurityService
                                    , nomadService: NomadService
                                    , consulService: ConsulService
                                    ) extends Controller with Logging {

  // TODO close on logout
  // TODO authentication with WebSocket.tryAccept
  // TODO authorization (how to manage that clients only receive updates they are allowed to)
  val user = Anonymous

  def socket = WebSocket.using[Msg] { request =>
    val (connectionId, connectionEnumerator) = webSocketService.newConnection() // TODO save also the user of this connection
    val connectionLogString = s"$connectionId ($request) from ${request.remoteAddress}"
    Logger.info(s"New connection $connectionLogString")
    // webSocketService.send(connectionId, "New templates and instances gogo.") only works after the enumerator and in has been passed
    // so when / where do we send the initial update?

    // TODO receive string and try json decoding here because I can handle the error better
    val in = Iteratee.foreach[Msg] {
        // TODO call the controller methods
      jsMsg =>
        Logger.info(s"Received message through $connectionId: $jsMsg")
        val msg = Json.fromJson(jsMsg)(IncomingWsMessage.incomingWsMessageReads)
        // TODO authentication
        val result = msg.map {
          case IncomingWsMessage(IncomingWsMessageType.AddInstance, instanceCreation: InstanceCreation) =>
            InstanceController.create(instanceCreation, user, instanceService) match {
              case success: InstanceCreationSuccess =>
                OutgoingWsMessage(OutgoingWsMessageType.InstanceCreationSuccessMsg, success)
              case failure: InstanceCreationFailure =>
                OutgoingWsMessage(OutgoingWsMessageType.InstanceCreationFailureMsg, failure)
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