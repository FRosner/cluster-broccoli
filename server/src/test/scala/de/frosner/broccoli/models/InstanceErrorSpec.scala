package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Role
import de.frosner.broccoli.http.ToHTTPResult
import org.specs2.ScalaCheck
import org.scalacheck.Gen
import play.api.libs.json.Json
import play.api.test.PlaySpecification

import scala.concurrent.Future

class InstanceErrorSpec
    extends PlaySpecification
    with ScalaCheck
    with ModelArbitraries
    with ToHTTPResult.ToToHTTPResultOps {
  "InstanceError" should {
    "serialize reason to JSON" in prop { error: InstanceError =>
      Json.toJson(error) === Json.obj("reason" -> error.reason)
    }

    "convert to an HTTP result with JSON body" in prop { error: InstanceError =>
      contentAsJson(Future.successful(error.toHTTPResult)) === Json.toJson(error)
    }
  }

  "InstanceError.NotFound" should {
    "map to 404" in prop { (id: String, throwable: Option[Throwable]) =>
      val error: InstanceError = InstanceError.NotFound(id, throwable)
      status(Future.successful(error.toHTTPResult)) === NOT_FOUND
    }.setGens(Gen.identifier.label("id"), Gen.option(Gen.identifier.label("message").map(new Throwable(_))))
  }

  "InstanceError.IdMissing" should {
    "map to bad request" in {
      status(Future.successful((InstanceError.IdMissing: InstanceError).toHTTPResult)) === BAD_REQUEST
    }
  }

  "InstanceError.TemplateNotFound" should {
    "map to bad request" in prop { id: String =>
      val error: InstanceError = InstanceError.TemplateNotFound(id)
      status(Future.successful(error.toHTTPResult)) === BAD_REQUEST
    }.setGen(Gen.identifier.label("templateId"))
  }

  "InstanceError.InvalidParameters" should {
    "map to bad request" in prop { reason: String =>
      val error: InstanceError = InstanceError.InvalidParameters(reason)
      status(Future.successful(error.toHTTPResult)) === BAD_REQUEST
    }.setGen(Gen.identifier.label("reason"))
  }

  "InstanceError.UserRegexDenied" should {
    "map to forbidden" in prop { (id: String, regex: String) =>
      val error: InstanceError = InstanceError.UserRegexDenied(id, regex)
      status(Future.successful(error.toHTTPResult)) === FORBIDDEN
    }.setGens(Gen.identifier.label("instanceId"), Gen.identifier.label("regex"))
  }

  "InstanceError.RolesRequest" should {
    "map to forbidden" in prop { roles: Set[Role] =>
      val error: InstanceError = InstanceError.RolesRequired(roles)
      status(Future.successful(error.toHTTPResult)) === FORBIDDEN
    }
  }

  "InstanceError.Generic" should {
    "map to bad request" in prop { message: String =>
      val error: InstanceError = InstanceError.Generic(new Throwable(message))
      status(Future.successful(error.toHTTPResult)) === BAD_REQUEST
    }.setGen(Gen.identifier.label("message"))
  }

}
