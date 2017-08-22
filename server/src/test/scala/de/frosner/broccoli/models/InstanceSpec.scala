package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import de.frosner.broccoli.services.InstanceService
import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.JsString

class InstanceSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {

  "The RemoveSecrets instance" should {
    "remove secret instance parameters" in prop { (instance: Instance) =>
      val publicInstance = instance.removeSecrets
      val (secret, public) = publicInstance.parameterValues.partition {
        case (id, _) =>
          instance.template.parameterInfos(id).secret.getOrElse(false)
      }
      (secret.values must contain(beNull[String]).foreach) and (public.values must contain(not(beNull[String])).foreach)
    }
  }

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"))
      val instance2 = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Heinz"))
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map.empty) must throwA[IllegalArgumentException]
    }

    "throw an exception if the template contains no default and no value" in {
      Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None, None, secret = Some(false), `type` = None))
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
