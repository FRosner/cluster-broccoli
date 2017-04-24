package de.frosner.broccoli.controllers

import javax.inject.Inject

import de.frosner.broccoli.conf
import de.frosner.broccoli.services._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.controllers.OutgoingWsMessageType._
import de.frosner.broccoli.util.Logging
import de.frosner.broccoli.models.Template.templateApiWrites
import de.frosner.broccoli.models.Instance.instanceApiWrites
import de.frosner.broccoli.models.{Anonymous, InstanceCreation, UserAccount}
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

  def socket = WebSocket.using[Msg] { request =>
    val (connectionId, connectionEnumerator) = webSocketService.newConnection() // TODO save also the user of this connection
    val connectionLogString = s"$connectionId ($request) from ${request.remoteAddress}"
    Logger.info(s"New connection $connectionLogString")
    // webSocketService.send(connectionId, "New templates and instances gogo.") only works after the enumerator and in has been passed
    // so when / where do we send the initial update?


    val in = Iteratee.foreach[Msg] {
        // TODO call the controller methods
      jsMsg =>
        Logger.info(s"Received message through $connectionId: $jsMsg")
        val msg = Try(Json.fromJson(jsMsg)(IncomingWsMessage.incomingWsMessageReads))
        msg.foreach { jsResult =>
          jsResult.foreach { message =>
            println(message)
          }
        }
        msg.foreach(_.foreach {
          case IncomingWsMessage(IncomingWsMessageType.AddInstance, instanceCreation: InstanceCreation) =>
            instanceService.addInstance(instanceCreation) // TODO use controller method
        })
    }.map { _ =>
      webSocketService.closeConnection(connectionId)
      Logger.info(s"Closed connection $connectionLogString")
    }

    // TODO reuse functionality of AboutController
    val user = Anonymous
    val aboutEnumerator = Enumerator[Msg](Json.toJson(
      OutgoingWsMessage(OutgoingWsMessageType.AboutInfoMsg, aboutService.aboutInfo(user))
    ))

    // TODO send request to set all instances and templates initially, then the webSocketService.channel will be used for subsequent updates
    val templateEnumerator = Enumerator[Msg](Json.toJson(
      OutgoingWsMessage(OutgoingWsMessageType.ListTemplatesMsg, templateService.getTemplates)
    ))

    // TODO reuse functionality in InstanceController
    val instanceEnumerator = Enumerator[Msg](Json.toJson(
      OutgoingWsMessage(OutgoingWsMessageType.ListInstancesMsg, instanceService.getInstances)
    ))
    (in, aboutEnumerator.andThen(templateEnumerator).andThen(instanceEnumerator).andThen(connectionEnumerator))
  }

}