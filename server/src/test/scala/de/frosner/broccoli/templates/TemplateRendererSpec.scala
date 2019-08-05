package de.frosner.broccoli.templates

import de.frosner.broccoli.models._
import com.hubspot.jinjava.JinjavaConfig
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{JsNumber, JsString}

import scala.collection.JavaConversions._

class TemplateRendererSpec extends Specification with Mockito {
  val templateRenderer =
    new TemplateRenderer(JinjavaConfig.newBuilder().withFailOnUnknownTokens(true).build())

  "TemplateRenderer" should {
    "render the template correctly when an instance contains a single parameter" in {
      val instance =
        Instance("1",
                 Template("1",
                          "\"{{id}}\"",
                          "desc",
                          "doc_url",
                          Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))),
                 Map("id" -> StringParameterValue("Frank")))
      templateRenderer.renderJson(instance) === JsString("Frank")
    }

    "parse the template correctly when it contains multiple parameters" in {
      val instance =
        Instance(
          "1",
          Template(
            "1",
            "\"{{id}} {{age}}\"",
            "desc",
            "doc_url",
            Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                "age" -> ParameterInfo("age", None, None, None, ParameterType.Integer, None))
          ),
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
          documentation_url = "doc_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
            "age" -> ParameterInfo("age",
                                   None,
                                   Some(IntParameterValue(50)),
                                   secret = Some(false),
                                   `type` = ParameterType.Integer,
                                   orderIndex = None)
          )
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
          documentation_url = "doc_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
                                  None,
                                  None,
                                  secret = Some(false),
                                  `type` = ParameterType.String,
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
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
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
          documentation_url = "doc_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
            "age" ->
              ParameterInfo("age", None, None, secret = Some(false), `type` = ParameterType.String, orderIndex = None)
          )
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
          documentation_url = "doc_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
                                  None,
                                  None,
                                  secret = Some(false),
                                  `type` = ParameterType.Decimal,
                                  orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> DecimalParameterValue(value))
      )
      templateRenderer.renderJson(instance) === JsNumber(value)
    }

    "parse correctly jinja2 local variables" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{% for x in [1,2,3] %}{{ id }}{{ x }}{% endfor %}\"",
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("1"))
      )
      templateRenderer.renderJson(instance) === JsString("111213")
    }

    "parse correctly jinja2 conditions" in {
      val template = "\"{% if id > 0 %}greater than zero{% else %}less than or equal to zero{% endif %}\""

      val instance1 = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = template,
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("10"))
      )
      templateRenderer.renderJson(instance1) === JsString("greater than zero")

      val instance2 = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = template,
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("-3"))
      )
      templateRenderer.renderJson(instance2) === JsString("less than or equal to zero")
    }

    "throws an exception if the template does not contain a value and default (withFailOnUnknownTokens = true)" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("Frank"))
      )

      templateRenderer.renderJson(instance) must throwA[FatalTemplateErrorsException]
    }

    "ignore undefined variables (withFailOnUnknownTokens = false)" in {
      val templateRenderer =
        new TemplateRenderer(JinjavaConfig.newBuilder().withFailOnUnknownTokens(false).build())
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("Frank"))
      )

      templateRenderer.renderJson(instance) === JsString("Frank ")
    }

    "parse the template correctly when it has Integer parameters" in {
      val value = 1234
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = """{{id}}""",
          description = "desc",
          documentation_url = "doc_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id",
                                  None,
                                  None,
                                  secret = Some(false),
                                  `type` = ParameterType.Integer,
                                  orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> IntParameterValue(value))
      )
      templateRenderer.renderJson(instance) === JsNumber(value)
    }
  }
}
