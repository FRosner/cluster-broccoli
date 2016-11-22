package de.frosner.broccoli.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import JobStatusJson.{instanceStatusReads, instanceStatusWrites}

class InstanceStatusSpec extends Specification {

  "Instance status JSON serialization" should {

    "work" in {
      val status = JobStatus.Running
      Json.fromJson(Json.toJson(status)).get === status
    }

  }

}
