package de.frosner.broccoli.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import Service.{serviceReads, serviceWrites}

class ServiceSpec extends Specification {

  "Service JSON serialization" should {

    "work" in {
      val service = Service(
        name = "s",
        protocol = "p",
        address = "a",
        port = 0,
        status = ServiceStatus.Unknown
      )
      Json.fromJson(Json.toJson(service)).get === service
    }

  }

}
