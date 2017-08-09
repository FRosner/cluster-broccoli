package de.frosner.broccoli.controllers

import javax.inject.Inject

import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import de.frosner.broccoli.controllers.OutgoingWsMessage._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import de.frosner.broccoli.util.Logging
import jp.t2v.lab.play2.auth.BroccoliWebsocketSecurity
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

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
      val in = Enumeratee.mapM[Msg] { incomingMessage =>
        Json
          .fromJson[IncomingWsMessage](incomingMessage)
          // We return a future here, since some messages map to asynchronous operations.
          .map[Future[OutgoingWsMessage]] {
            case IncomingWsMessage.AddInstance(instanceCreation) =>
              InstanceController
                .create(instanceCreation, user, instanceService)
                // We lift the plain Either returned by ".create" into an EitherT and thus into a "Future", so that the
                // subsequent ".fold" gives us not just an OutgoingWsMessage, but a Future[OutgoingWsMessage]
                .toEitherT
                .fold(AddInstanceError, AddInstanceSuccess)
            case IncomingWsMessage.DeleteInstance(instanceId) =>
              InstanceController
                .delete(instanceId, user, instanceService)
                .toEitherT
                .fold(DeleteInstanceError, DeleteInstanceSuccess)
            case IncomingWsMessage.UpdateInstance(instanceUpdate) =>
              InstanceController
                .update(instanceUpdate.instanceId.get, instanceUpdate, user, instanceService)
                .toEitherT
                .fold(UpdateInstanceError, UpdateInstanceSuccess)
            case IncomingWsMessage.GetInstanceTasks(instanceId) =>
              instanceService
                .getInstanceTasks(user)(instanceId)
                .fold(GetInstanceTasksError, GetInstanceTasksSuccess)
          }
          .recoverTotal { error =>
            Logger.warn(s"Can't parse a message from $connectionId: $error")
            Future.successful(OutgoingWsMessage.Error(s"Failed to parse message message: $error"))
          }
      } transform Iteratee
        .foreach { outgoingMessage: OutgoingWsMessage =>
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
