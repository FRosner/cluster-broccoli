package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import de.frosner.broccoli.services.InstanceService
import org.specs2.mutable.Specification
import org.specs2.mutable._
import play.api.libs.json.JsString

class InstanceSpec extends Specification {

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      val instance2 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map.empty, InstanceStatus.Unknown, Map.empty) must throwA[IllegalArgumentException]
    }

    "parse the template correctly when it contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Frank"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance = Instance("1", Template("1", "\"{{id}} {{age}}\"", "desc", Map.empty), Map("id" -> "Frank", "age" -> "5"), InstanceStatus.Unknown, Map.empty)
      instance.templateJson === JsString("Frank 5")
    }

    "parse the template correctly when it contains a defined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", Some("50")))
        ),
        parameterValues = Map("id" -> "Frank"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      instance.templateJson === JsString("Frank 50")
    }

    "parse the template correctly when it contains an undefined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None))
        ),
        parameterValues = Map("id" -> "Frank", "age" -> "50"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      instance.templateJson === JsString("Frank 50")
    }

    "throw an exception if the template contains no default and no value" in {
      Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None))
        ),
        parameterValues = Map("id" -> "Frank"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      ) must throwA[IllegalArgumentException]
    }

  }

  "Updating the parameters of an instance" should {

    "work correctly" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map.empty
        ),
        parameterValues = Map("id" -> "1", "age" -> "50"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      val newParameterValues = Map("id" -> "1", "age" -> "30")
      val newInstance = instance.updateParameterValues(newParameterValues)
      newInstance.get.parameterValues === newParameterValues
    }

    "not allow changing of the ID" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map.empty
        ),
        parameterValues = Map("id" -> "1", "age" -> "50"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      val newParameterValues = Map("id" -> "2", "age" -> "40")
      val newInstance = instance.updateParameterValues(newParameterValues)
      newInstance.isFailure === true
    }

    "require parameter and value consistency" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map.empty
        ),
        parameterValues = Map("id" -> "1", "age" -> "50"),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      val newParameterValues = Map("id" -> "1")
      val newInstance = instance.updateParameterValues(newParameterValues)
      newInstance.isFailure === true
    }

  }

  "Instance serialization" should {

    "work correctly" in {
      val original = Map(
        "1" -> Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"), InstanceStatus.Unknown, Map.empty)
      )

      val bos = new ByteArrayOutputStream()
      InstanceService.persistInstances(original, bos)
      val deserialized = InstanceService.loadInstances(new ByteArrayInputStream(bos.toByteArray)).get

      original === deserialized
    }

  }

}
