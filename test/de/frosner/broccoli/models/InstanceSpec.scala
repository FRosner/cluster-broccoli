package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import de.frosner.broccoli.services.InstanceService
import org.specs2.mutable.Specification
import org.specs2.mutable._
import play.api.libs.json.JsString

class InstanceSpec extends Specification {

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "\"{{id}}\"", "desc"), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      val instance2 = Instance("1", Template("1", "\"{{id}}\"", "desc"), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "\"{{id}}\"", "desc"), Map.empty, InstanceStatus.Unknown, Map.empty) must throwA[IllegalArgumentException]
    }

    "parse the template correctly when it contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc"), Map("id" -> "Frank"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank")
    }

    "parse the template correctly when it contains a single parameter multiple times" in {
      val instance = Instance("1", Template("1", "\"{{id}}     {{id}} foo {{id}} {{id}} bar\"", "desc"), Map("id" -> "Frank"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank     Frank foo Frank Frank bar")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance = Instance("1", Template("1", "\"{{id}} {{age}} {{height}}\"", "desc"), Map("id" -> "Frank", "age" -> "500000000", "height" -> "50"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank 500000000 50")
    }

    "parse the template correctly when it contains a single parameter with advanced syntax" in {
      val instance = Instance("1", Template("1", "\"{{name:id}}\"", "desc"), Map("id" -> "Frank"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters with advanced syntax" in {
      val instance = Instance("1", Template("1", "\"{{name:id}} {{name:age}}\"", "desc"), Map("id" -> "Frank", "age" -> "5"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank 5")
    }

  }

  "Instance serialization" should {

    "work correctly" in {
      val original = Map(
        "1" -> Instance("1", Template("1", "\"{{id}}\"", "desc"), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      )

      val bos = new ByteArrayOutputStream()
      InstanceService.persistInstances(original, bos)
      val deserialized = InstanceService.loadInstances(new ByteArrayInputStream(bos.toByteArray)).get

      original === deserialized
    }

  }

}
