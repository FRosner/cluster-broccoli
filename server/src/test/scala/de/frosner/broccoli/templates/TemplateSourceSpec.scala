package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{ParameterInfo, Template}
import de.frosner.broccoli.templates.TemplateConfig.{Parameter, TemplateInfo}
import org.specs2.mutable.Specification

class TemplateSourceSpec extends Specification {
  "loadTemplate" should {
    val templateSource = new TemplateSource() {
      override def loadTemplates(): Seq[Template] = Seq.empty
    }

    "work" in {
      val id = "templateId"
      val templateString = "Hello {{ id }}"
      val templateInfo = TemplateInfo(None, Map("id" -> Parameter(Some("id"), None, None, None, None)))
      val convertDashesToUnderscores = false
      val template =
        templateSource.loadTemplate(id, templateString, templateInfo, convertDashesToUnderscores).get

      template.id === id and template.template === templateString and template.parameterInfos("id") === ParameterInfo
        .fromTemplateInfoParameter("id", templateInfo.parameters.get("id").get)
    }

    "convert dashes in 'legacy' variables to underscores if convertDashesToUnderscores is true" in {
      val tryTemplate =
        templateSource.loadTemplate(
          "templateId",
          "Hello {{id}} {{custom-var}}",
          TemplateInfo(None,
                       Map("id" -> Parameter(Some("id"), None, None, None, None),
                           "custom_var" -> Parameter(Some("custom_var"), None, None, None, None))),
          true
        )
      (tryTemplate.isSuccess must beTrue) and (tryTemplate.get.parameters must contain("custom_var"))
    }

    "require an 'id' parameter (no parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate("test", "Hallo", TemplateInfo(None, Map.empty), false)
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set()"))
    }

    "require an 'id' parameter (wrong parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate("test",
                                    "Hallo {{bla}}",
                                    TemplateInfo(None, Map("bla" -> Parameter(Some("bla"), None, None, None, None))),
                                    false)
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set(bla)"))
    }

    "not expose variables that are not defined in the template config" in {
      val tryTemplate =
        templateSource.loadTemplate(
          "test",
          "{{id}} {{ global_var }} {% for x in [1 2 3] %}{{x}}{% endfor %}",
          TemplateInfo(None,
                       Map("id" -> Parameter(Some("id"), None, None, None, None),
                           "global_var" -> Parameter(Some("global_var"), None, None, None, None))),
          false
        )
      (tryTemplate.isSuccess must beTrue) and (tryTemplate.get.parameters must not contain "x")
    }
  }
}
