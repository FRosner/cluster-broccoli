package de.frosner.broccoli.templates

import de.frosner.broccoli.models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{JsNumber, JsString}

import scala.util.{Failure, Try}

class TemplateRendererSpec extends Specification with Mockito {
  val templateRenderer = new TemplateRenderer(ParameterType.Raw)

  "TemplateRenderer" should {
    "render the template correctly when an instance contains a single parameter" in {
      val instance = Instance("1", Template("1", "\"{{id}}\"", "desc", Map.empty), Map("id" -> StringParameterValue("Frank")))
      templateRenderer.renderJson(instance) === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance =
        Instance(
          "1",
          Template("1", "\"{{id}} {{age}}\"", "desc", Map.empty),
          Map("id" -> RawParameterValue("Frank"), "age" -> IntParameterValue(5))
        )
      templateRenderer.renderJson(instance) === JsString("Frank 5")
    }

    "parse the template correctly when it contains a defined default parameter" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos =
            Map("age" -> ParameterInfo("age", None, Some("50"), secret = Some(false), `type` = None, orderIndex = None))
        ),
        parameterValues = Map("id" -> RawParameterValue("Frank"))
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
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
                                  None,
                                  None,
                                  secret = Some(false),
                                  `type` = Some(ParameterType.String),
                                  orderIndex = None))
        ),
        parameterValues = Map("id" -> StringParameterValue("\"Frank"))
      )
      templateRenderer.renderJson(instance) === JsString("\"Frank")
    }

    "parse the template correctly when it contains regex stuff that breaks with replaceAll" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}}\"",
          description = "desc",
          parameterInfos = Map.empty
        ),
        parameterValues = Map("id" -> RawParameterValue("^.*$"))
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
          parameterInfos =
            Map("age" -> ParameterInfo("age", None, None, secret = Some(false), `type` = None, orderIndex = None))
        ),
        parameterValues = Map("id" -> StringParameterValue("Frank"), "age" -> IntParameterValue(50))
      )
      templateRenderer.renderJson(instance) === JsString("Frank 50")
    }

    "parse the template correctly when it has Decimal parameters" in {
      val value = 1234.56
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = """{{id}}""",
          description = "desc",
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
              None,
              None,
              secret = Some(false),
              `type` = Some(ParameterType.Decimal),
              orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> DecimalParameterValue(value))
      )
      templateRenderer.renderJson(instance) === JsNumber(value)
    }

    "parse the template correctly when it has Integer parameters" in {
      val value = 1234
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = """{{id}}""",
          description = "desc",
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
              None,
              None,
              secret = Some(false),
              `type` = Some(ParameterType.Integer),
              orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> IntParameterValue(value))
      )
      templateRenderer.renderJson(instance) === JsNumber(value)
    }
  }
}
