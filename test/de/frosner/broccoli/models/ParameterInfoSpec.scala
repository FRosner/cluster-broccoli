package de.frosner.broccoli.models

import de.frosner.broccoli.models.ParameterInfo.{parameterInfoWrites, parameterInfoReads}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ParameterInfoSpec extends Specification {

  "Service status JSON serialization" should {

    "work" in {
      val info = ParameterInfo(id = "i", default = Some("d"), secret = Some(false))
      Json.fromJson(Json.toJson(info)).get === info
    }

  }

}
