package de.frosner.broccoli.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import InstanceStatusJson.{instanceStatusReads, instanceStatusWrites}

class InstanceStatusSpec extends Specification {

  "Instance status JSON serialization" should {

    "work" in {
      val status = InstanceStatus.Running
      Json.fromJson(Json.toJson(status)).get === status
    }

  }

}
