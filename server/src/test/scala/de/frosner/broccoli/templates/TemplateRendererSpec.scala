package de.frosner.broccoli.templates

import de.frosner.broccoli.instances.StorageType
import de.frosner.broccoli.instances.conf.{
  CouchDBInstanceStorageConfiguration,
  FileSystemInstanceStorageConfiguration,
  InstanceConfiguration,
  InstanceStorageConfiguration
}
import de.frosner.broccoli.models.{Instance, ParameterInfo, ParameterType, Template}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.JsString

class TemplateRendererSpec extends Specification with Mockito {
  val templateRenderer = new TemplateRenderer(ParameterType.Raw)

  "TemplateRenderer" should {
    "render the template correctly when an instance contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Frank"))
      templateRenderer.renderJson(instance) === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance =
        Instance("1", Template("1", "\"{{id}} {{age}}\"", "desc", Map.empty), Map("id" -> "Frank", "age" -> "5"))
      templateRenderer.renderJson(instance) === JsString("Frank 5")
    }

    "parse the template correctly when it contains a defined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None, Some("50"), secret = Some(false), `type` = None))
        ),
        parameterValues = Map("id" -> "Frank")
      )
      templateRenderer.renderJson(instance) === JsString("Frank 50")
    }

    "parse the template correctly when it has String parameters" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = """"{{id}}"""",
          description = "desc",
          parameterInfos =
            Map("id" -> ParameterInfo("id", None, None, secret = Some(false), `type` = Some(ParameterType.String)))
        ),
        parameterValues = Map("id" -> "\"Frank")
      )
      templateRenderer.renderJson(instance) === JsString("\"Frank")
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
      templateRenderer.renderJson(instance) === JsString("^.*$")
    }

    "parse the template correctly when it contains an undefined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("age" -> ParameterInfo("age", None, None, secret = Some(false), `type` = None))
        ),
        parameterValues = Map("id" -> "Frank", "age" -> "50")
      )
      templateRenderer.renderJson(instance) === JsString("Frank 50")
    }
  }

  "sanitize" should {
    val parameterValue = "value"
    "just return the specified parameter value if it is raw" in {
      templateRenderer
        .sanitize(
          "parameter",
          parameterValue,
          Map("parameter" -> ParameterInfo("parameter", None, None, None, Some(ParameterType.Raw)))) === parameterValue
    }

    "escape the value if it is a string" in {
      templateRenderer
        .sanitize(
          "parameter",
          """ "value  """,
          Map("parameter" -> ParameterInfo("parameter", None, None, None, Some(ParameterType.String)))) === """ \"value  """
    }

    "pick the default parameter type if the type for the parameter is not specified in ParameterInfos" in {
      templateRenderer
        .sanitize("parameter", parameterValue, Map.empty) === parameterValue

    }

  }
}
