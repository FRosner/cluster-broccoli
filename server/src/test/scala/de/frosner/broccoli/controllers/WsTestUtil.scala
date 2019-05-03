package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import de.frosner.broccoli.services.WebSocketService.Msg
import play.api.mvc.Result

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

object WsTestUtil {

  private val waitTime = FiniteDuration(1, TimeUnit.SECONDS)

  implicit val actorSystem = ActorSystem("WsTestUtil")
  implicit val materializer = ActorMaterializer()

  case class Incoming(flow: Flow[Msg, Msg, _], messages: List[Msg]) {

    def feed(message: Msg): Incoming =
      Incoming(flow, message :: messages)

    def end: Incoming = {
      val r = Source(messages.reverse).viaMat(flow)(Keep.both).toMat(Sink.seq)(Keep.both)
      val (_, future) = r.run()
      Await.result(future, waitTime)
      Incoming(flow, List())
    }
  }

  object Incoming {
    def apply(flow: Flow[Msg, Msg, _]): Incoming = new Incoming(flow, List())
  }

  case class Outgoing(flow: Flow[Msg, Msg, _]) {
    private val messages = flow.runWith(Source.empty, Sink.fold(List[Msg]())((l, e) => e :: l)) match {
      case (_, msgs) => msgs
    }

    def get: List[Msg] =
      Await.result(messages, waitTime)
  }

  def wrapConnection(connection: => Future[Either[Result, Flow[Msg, Msg, _]]]): Either[Result, (Incoming, Outgoing)] = {
    val future = connection.map {
      _.right.map(flow => (Incoming(flow), Outgoing(flow)))
    }
    Await.result(future, waitTime)
  }
}
