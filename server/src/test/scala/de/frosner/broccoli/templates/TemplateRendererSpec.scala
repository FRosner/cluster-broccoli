package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{Instance, ParameterInfo, ParameterType, Template}
import org.specs2.mutable.Specification
import play.api.libs.json.JsString

class TemplateRendererSpec extends Specification {
  "TemplateRenderer" should {
    val templateRenderer = new TemplateRenderer()

    "render the template correctly when an instance contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> "Frank"))
      templateRenderer.templateJson(instance) === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance =
        Instance("1", Template("1", "\"{{id}} {{age}}\"", "desc", Map.empty), Map("id" -> "Frank", "age" -> "5"))
      templateRenderer.templateJson(instance) === JsString("Frank 5")
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
      templateRenderer.templateJson(instance) === JsString("Frank 50")
    }

    "parse the template correctly when it has String parameters" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "{{id}}",
          description = "desc",
          parameterInfos =
            Map("id" -> ParameterInfo("id", None, None, secret = Some(false), `type` = Some(ParameterType.String)))
        ),
        parameterValues = Map("id" -> "Frank")
      )
      templateRenderer.templateJson(instance) === JsString("Frank")
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
      templateRenderer.templateJson(instance) === JsString("^.*$")
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
      templateRenderer.templateJson(instance) === JsString("Frank 50")
    }
  }
}
