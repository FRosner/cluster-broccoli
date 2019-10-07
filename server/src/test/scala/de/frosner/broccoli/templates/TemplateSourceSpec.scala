package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{ParameterInfo, ParameterType, Template}
import de.frosner.broccoli.templates.TemplateConfig.{Parameter, TemplateInfo}
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import org.specs2.mock.Mockito

class TemplateSourceSpec extends Specification with Mockito {
  "loadTemplate" should {

    def templateRenderer(areValid: Boolean, paramNames: Seq[String]): TemplateRenderer = {
      val templateRenderer = mock[TemplateRenderer]
      when(templateRenderer.validateParameterName(anyString)).thenReturn(!areValid)
      paramNames.foreach(
        name => when(templateRenderer.validateParameterName(name)).thenReturn(areValid)
      )
      templateRenderer
    }
    // Always true when validating
    def defaultTemplateRenderer: TemplateRenderer = templateRenderer(false, Seq())

    def templateSource(renderer: TemplateRenderer): TemplateSource = new TemplateSource {
      val templateRenderer: TemplateRenderer = renderer
      override def loadTemplates(refreshTemplate: Boolean): Seq[Template] = Seq.empty
    }
    def defaultTemplateSource = templateSource(defaultTemplateRenderer)

    "work" in {
      val id = "templateId"
      val templateString = "Hello {{ id }}"
      val templateInfo = TemplateInfo(None, Map("id" -> Parameter(Some("id"), None, None, ParameterType.Raw, None)))
      val template =
        defaultTemplateSource.loadTemplate(id, templateString, templateInfo).get

      template.id === id and template.template === templateString and template.parameterInfos("id") === ParameterInfo
        .fromTemplateInfoParameter("id", templateInfo.parameters.get("id").get)
    }

    "require an 'id' parameter (no parameter)" in {
      val tryTemplate =
        defaultTemplateSource.loadTemplate("test", "Hallo", TemplateInfo(None, Map.empty))
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set()"))
    }

    "require an 'id' parameter (wrong parameter)" in {
      val tryTemplate =
        defaultTemplateSource.loadTemplate(
          "test",
          "Hallo {{bla}}",
          TemplateInfo(None, Map("bla" -> Parameter(Some("bla"), None, None, ParameterType.Raw, None)))
        )
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set(bla)"))
    }

    "should fail when template info contains an invalid parameter name" in {
      val tryTemplate =
        templateSource(templateRenderer(false, Seq("bla-bla"))).loadTemplate(
          "test",
          "Hallo {{bla-bla}}",
          TemplateInfo(None,
                       Map("id" -> Parameter(Some("id"), None, None, ParameterType.Raw, None),
                           "bla-bla" -> Parameter(Some("bla"), None, None, ParameterType.Raw, None)))
        )
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        s"requirement failed: The following parameters are invalid: bla-bla"))
    }

    "not expose variables that are not defined in the template config" in {
      val tryTemplate =
        defaultTemplateSource.loadTemplate(
          "test",
          "{{id}} {{ global_var }} {% for x in [1 2 3] %}{{x}}{% endfor %}",
          TemplateInfo(None,
                       Map("id" -> Parameter(Some("id"), None, None, ParameterType.Raw, None),
                           "global_var" -> Parameter(Some("global_var"), None, None, ParameterType.Raw, None)))
        )
      (tryTemplate.isSuccess must beTrue) and (tryTemplate.get.parameters must not contain "x")
    }
  }
}
