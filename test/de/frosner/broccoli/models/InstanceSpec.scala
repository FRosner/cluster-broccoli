package de.frosner.broccoli.models

import org.specs2.mutable.Specification
import org.specs2.mutable._
import play.api.libs.json.JsString

class InstanceSpec extends Specification {

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "\"{{name}}\"", "desc"), Map("name" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      val instance2 = Instance("1", Template("1", "\"{{name}}\"", "desc"), Map("name" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "\"{{name}}\"", "desc"), Map.empty, InstanceStatus.Unknown, Map.empty) must throwA[IllegalArgumentException]
    }

    "parse the template correctly when it contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{name}}\"", "desc"), Map("name" -> "Frank"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance = Instance("1", Template("1", "\"{{name}} {{age}}\"", "desc"), Map("name" -> "Frank", "age" -> "5"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank 5")
    }

    "parse the template correctly when it contains no parameter" in {
      val instance = Instance("1", Template("1", "\"name\"", "desc"), Map.empty, InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("name")
    }

  }

}
