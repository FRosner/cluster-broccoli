package de.frosner.broccoli.http

import cats.syntax.either._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import play.api.mvc.Results
import play.api.test.PlaySpecification

import scala.concurrent.Future

class ToHTTPResultSpec extends PlaySpecification with ScalaCheck {
  import ToHTTPResult.ops._

  "ToHTTPResult.instance" should {
    "convert to a result with the given function" in prop { (body: String, statusCode: Int) =>
      implicit val instance = ToHTTPResult.instance[String](Results.Status(statusCode)(_))

      val result = Future.successful(body.toHTTPResult)
      (status(result) === statusCode) and (contentAsString(result) === body)
    }.setGens(Gen.identifier.label("value"), Gen.choose(200, 500).label("status"))
  }

  "ToHTTPResult Either instance" should {
    "convert to a result of either left or right" in prop { (value: Either[String, String]) =>
      implicit val stringToHTTPResult = ToHTTPResult.instance[String](Results.Ok(_))
      val result = Future.successful(value.toHTTPResult)
      contentAsString(result) === value.merge
    }.setGen(
      Gen.oneOf(Gen.identifier.map(Either.left).label("left"), Gen.identifier.map(Either.right).label("right"))
    )
  }
}
