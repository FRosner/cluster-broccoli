package de.frosner.broccoli.controllers

import akka.stream.javadsl.Sink
import akka.stream.scaladsl.{Flow, Source}
import javax.inject.Inject
import cats.data.EitherT
import cats.implicits._
import com.mohiva.play.silhouette.api.Silhouette
import de.frosner.broccoli.auth.{BroccoliWebsocketSecurity, DefaultEnv}
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import de.frosner.broccoli.websocket.{IncomingMessage, OutgoingMessage, WebSocketMessageHandler}
import play.api.cache.SyncCacheApi
import play.api.libs.iteratee._
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.json._
import play.api.mvc._
import play.api.{Environment, Logger}

import scala.concurrent.{ExecutionContext, Future}

case class WebSocketController @Inject()(webSocketService: WebSocketService,
                                         templateService: TemplateService,
                                         instanceService: InstanceService,
                                         aboutService: AboutInfoService,
                                         nomadService: NomadService,
                                         messageHandler: WebSocketMessageHandler,
                                         override val controllerComponents: ControllerComponents,
                                         override val silhouette: Silhouette[DefaultEnv],
                                         override val cacheApi: SyncCacheApi,
                                         override val playEnv: Environment,
                                         override val securityService: SecurityService,
                                         override implicit val executionContext: ExecutionContext)
    extends BaseController
    with BroccoliWebsocketSecurity {

  protected val log = Logger(getClass)

  def requestToSocket(request: RequestHeader): Future[Either[Result, Flow[Msg, Msg, _]]] =
    withSecurity(request) { (maybeToken, user, request) =>
      val (connectionId, connectionEnumerator) = maybeToken match {
        case Some(token) =>
          (token, webSocketService.newConnection(token, user)) // auth is enabled and we can use the session ID
        case None => webSocketService.newConnection(user) // no session ID available so we generate one
      }
      val connectionLogString = s"$connectionId by $user from ${request.remoteAddress} at $request"
      log.info(s"New connection $connectionLogString")

      // TODO receive string and try json decoding here because I can handle the error better
      val iteratee = Enumeratee.mapM[Msg] { incomingMessage =>
        EitherT
          .fromEither[Future](Json.fromJson[IncomingMessage](incomingMessage).asEither)
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
      val (subscriber, _) = IterateeStreams.iterateeToSubscriber(iteratee)
      val in = Sink.fromSubscriber[Msg](subscriber)

      val aboutSource = Source.single[Msg](
        Json.toJson(
          OutgoingMessage.AboutInfoMsg(AboutController.about(aboutService, user))
        ))
      val templateSource = Source.single[Msg](
        Json.toJson(
          OutgoingMessage.ListTemplates(TemplateController.list(templateService), user)
        ))
      val instanceSource = Source.single[Msg](
        Json.toJson(
          OutgoingMessage.ListInstances(InstanceController.list(None, user, instanceService), user)
        ))
      val connectionSource = Source.fromPublisher(IterateeStreams.enumeratorToPublisher(connectionEnumerator))

      Flow.fromSinkAndSource(in, aboutSource.concat(templateSource).concat(instanceSource).concat(connectionSource))
    }

  def socket: WebSocket = WebSocket.acceptOrResult[Msg, Msg](requestToSocket)

}
