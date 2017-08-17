package de.frosner.broccoli

import org.mockito.{ArgumentCaptor, Matchers}
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.util.matching.Regex

class LoggingSpec extends Specification with Mockito with ScalaCheck {
  import logging._

  trait F[T] {
    def body(): T

    def log(message: String): Unit
  }

  "logging the execution time" should {

    "execute the block just once" in {
      val f = mock[F[Unit]]

      logExecutionTime("foo") {
        f.body()
      }(Function.const(()))

      there was one(f).body()
      there was no(f).log(Matchers.any[String]())
    }

    "invokes the log function" in prop { label: String =>
      val f = mock[F[Int]]

      logExecutionTime(label) {
        42
      }(f.log(_))

      val message = ArgumentCaptor.forClass(classOf[String])
      there was one(f).log(message.capture())
      message.getValue must beMatching(s"${Regex.quote(label)} took \\d+ ms")

      there was no(f).body()
    }.setGen(Gen.identifier.label("label"))

    "returns the result of the body" in prop { ret: Int =>
      logExecutionTime("foo") {
        ret
      }(Function.const(())) === ret
    }
  }

}
