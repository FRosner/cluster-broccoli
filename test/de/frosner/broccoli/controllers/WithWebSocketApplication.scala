package de.frosner.broccoli.controllers

import java.util.concurrent.TimeUnit

import play.api.libs.iteratee.{Enumerator, Input, Iteratee}
import play.api.mvc.Result
import play.api.test.WithApplication

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

trait WithWebSocketApplication[Msg] extends WithApplication {

  private val waitTime = FiniteDuration(1, TimeUnit.SECONDS)

  case class Incoming(iteratee: Iteratee[Msg, _]) {

    def feed(message: Msg): Incoming =
      Incoming(Await.result(iteratee.feed(Input.El(message)), waitTime))

    def end: Incoming =
      Incoming(Await.result(iteratee.feed(Input.EOF), waitTime))

  }

  case class Outgoing(enum: Enumerator[Msg]) {
    private val messages = enum(Iteratee.fold(List[Msg]()) {
      (l, jsValue) => jsValue :: l
    }).flatMap(_.run)

    def get: List[Msg] = {
      Await.result(messages, waitTime)
    }
  }

  def wrapConnection(connection: => Future[Either[Result, (Iteratee[Msg, _], Enumerator[Msg])]]): Either[Result, (Incoming, Outgoing)] = {
    val future = connection.map {
      _.right.map {
        case (iteratee, enumerator) => (Incoming(iteratee), Outgoing(enumerator))
      }
    }
    Await.result(future, waitTime)
  }

}