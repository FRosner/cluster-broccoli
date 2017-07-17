package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import de.frosner.broccoli.services.InstanceService
import org.specs2.mutable.Specification
import org.specs2.mutable._
import play.api.libs.json.{JsString, Json}

class InstanceSpec extends Specification {

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"))
      val instance2 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"))
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map.empty) must throwA[IllegalArgumentException]
    }

    "parse the template correctly when it contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Frank"))
      instance.templateJson === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance =
        Instance("1", Template("1", "\"{{id}} {{age}}\"", "desc", Map.empty), Map("id" -> "Frank", "age" -> "5"))
      instance.templateJson === JsString("Frank 5")
    }

    "parse the template correctly when it contains a defined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None, Some("50"), secret = Some(false)))
        ),
        parameterValues = Map("id" -> "Frank")
      )
      instance.templateJson === JsString("Frank 50")
    }

    "parse the template correctly when it contains regex stuff that breacks with replaceAll" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}}\"",
          description = "desc",
          parameterInfos = Map.empty
        ),
        parameterValues = Map("id" -> "^.*$")
      )
      instance.templateJson === JsString("^.*$")
    }

    "parse the template correctly when it contains an undefined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None, None, secret = Some(false)))
        ),
        parameterValues = Map("id" -> "Frank", "age" -> "50")
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
          parameterInfos = Map("age" -> ParameterInfo("age", None, None, secret = Some(false)))
        ),
        parameterValues = Map("id" -> "Frank")
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
        parameterValues = Map("id" -> "1", "age" -> "50")
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
        parameterValues = Map("id" -> "1", "age" -> "50")
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
        parameterValues = Map("id" -> "1", "age" -> "50")
      )
      val newParameterValues = Map("id" -> "1")
      val newInstance = instance.updateParameterValues(newParameterValues)
      newInstance.isFailure === true
    }

  }

  "Updating the template of an instance" should {

    "work correctly when the parameters don't change" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val originalParameterValues = Map("id" -> "1", "age" -> "50")
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = instance.updateTemplate(newTemplate, newParameterValues)
      (newInstance.get.template === newTemplate) and (newInstance.get.parameterValues === newParameterValues)
    }

    "work correctly when the parameters change" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val originalParameterValues = Map("id" -> "1", "age" -> "50")
      val newParameterValues = originalParameterValues.updated("height", "170")
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = instance.updateTemplate(newTemplate, newParameterValues)
      (newInstance.get.template === newTemplate) and (newInstance.get.parameterValues === newParameterValues)
    }

    "require parameter and value consistency" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val originalParameterValues = Map("id" -> "1", "age" -> "50")
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = instance.updateTemplate(newTemplate, newParameterValues)
      (newInstance.isFailure === true) and (instance.template === originalTemplate)
    }

    "not allow changing of the ID" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map.empty
      )
      val originalParameterValues = Map("id" -> "2", "age" -> "50")
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = instance.updateTemplate(newTemplate, newParameterValues)
      newInstance.isFailure === true
    }

  }

}
