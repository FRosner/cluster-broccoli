package de.frosner.broccoli.models

import de.frosner.broccoli.models.ParameterInfo.{parameterInfoReads, parameterInfoWrites}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ParameterInfoSpec extends Specification {

  "Service status JSON serialization" should {

    "work" in {
      val info = ParameterInfo(id = "i",
                               name = None,
                               default = Some("d"),
                               secret = Some(false),
                               `type` = Some(ParameterType.String),
                               orderIndex = None)
      Json.fromJson(Json.toJson(info)).get === info
    }

  }

}
