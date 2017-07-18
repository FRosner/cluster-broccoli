package de.frosner.broccoli.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import ServiceStatusJson.{serviceStatusReads, serviceStatusWrites}

class ServiceStatusSpec extends Specification {

  "Service status JSON serialization" should {

    "work" in {
      val status = ServiceStatus.Passing
      Json.fromJson(Json.toJson(status)).get === status
    }

  }

}
