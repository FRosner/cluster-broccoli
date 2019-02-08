package de.frosner.broccoli.controllers


import akka.actor.{Actor, ActorRef, Props}
import javax.inject.Inject
import cats.data.EitherT
import cats.instances.future._
import com.mohiva.play.silhouette
import com.mohiva.play.silhouette.api.{HandlerResult, Silhouette}
import de.frosner.broccoli.auth.{Account, DefaultEnv}
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import de.frosner.broccoli.websocket.{IncomingMessage, OutgoingMessage, WebSocketMessageHandler}
import jp.t2v.lab.play2.auth.BroccoliWebsocketSecurity
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import play.api.{Environment, Logger}

import scala.concurrent.Future

case class WebSocketController @Inject()(webSocketService: WebSocketService,
                                         templateService: TemplateService,
                                         instanceService: InstanceService,
                                         aboutService: AboutInfoService,
                                         nomadService: NomadService,
                                         messageHandler: WebSocketMessageHandler,
                                         silhouette: Silhouette[DefaultEnv],
                                         override val cacheApi: CacheApi,
                                         override val playEnv: Environment,
                                         override val securityService: SecurityService)
    extends Controller  {

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
        EitherT
          .fromEither(Json.fromJson[IncomingMessage](incomingMessage).asEither)
          .leftMap[OutgoingMessage] { jsonErrors =>
            log.warn(s"Can't parse a message from $connectionId: $jsonErrors")
            OutgoingMessage.Error(s"Failed to parse message message: $jsonErrors")
          }
          .semiflatMap { incomingMessage =>
            // Catch all exceptions from the message handler and map them to a generic error message to send over the
            // websocket, to prevent the Enumeratee from stopping at the failure, causing the websocket to be closed and
            // preventing all future messages.
            messageHandler.processMessage(user)(incomingMessage).recover {
              case exception =>
                log.error(s"Message handler threw exception for message $incomingMessage: ${exception.getMessage}",
                          exception)
                OutgoingMessage.Error("Unexpected error in message handler")
            }
          }
          .merge
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
          OutgoingMessage.ListTemplates(TemplateController.list(templateService), user)
        ))

      val instanceEnumerator = Enumerator[Msg](
        Json.toJson(OutgoingMessage.ListInstances(InstanceController.list(None, user, instanceService), user)))

      (in,
       aboutEnumerator
         .andThen(templateEnumerator)
         .andThen(instanceEnumerator)
         .andThen(connectionEnumerator))
    }

  def socket: WebSocket = WebSocket.tryAccept[Msg](requestToSocket)

  object WebSocketActor {
    def props(account: Account)(out: ActorRef) = Props(new WebSocketActor())
  }

  class WebSocketActor(account: Account, out: ActorRef) extends Actor {
    override def receive: Receive = {
      case incomingMessage: JsValue => {
        val a = EitherT
          .fromEither(Json.fromJson[IncomingMessage](incomingMessage).asEither)
          .leftMap[OutgoingMessage] { jsonErrors =>
          log.warn(s"Can't parse a message from ${account.name}: $jsonErrors")
          OutgoingMessage.Error(s"Failed to parse message message: $jsonErrors")
        }
          .semiflatMap { incomingMessage =>
            // Catch all exceptions from the message handler and map them to a generic error message to send over the
            // websocket, to prevent the Enumeratee from stopping at the failure, causing the websocket to be closed and
            // preventing all future messages.
            messageHandler.processMessage(account)(incomingMessage).recover {
              case exception =>
                log.error(s"Message handler threw exception for message $incomingMessage: ${exception.getMessage}",
                  exception)
                OutgoingMessage.Error("Unexpected error in message handler")
            }
          }
          .merge
        a
      }
    }
  }

  def newSocket: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue](request => {
    implicit val req = Request(request, AnyContentAsEmpty)
    val w = silhouette.SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Right(ActorFlow.actorRef(WebSocketActor.props(user)))
      case HandlerResult(r, None) => Left(r)
    }
    w
  })

}
