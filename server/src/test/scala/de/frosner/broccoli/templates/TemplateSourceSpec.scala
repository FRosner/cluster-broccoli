package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{ParameterInfo, ParameterType, Template}
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
      val templateInfo = TemplateInfo(None, Map("id" -> Parameter(Some("id"), None, None, ParameterType.Raw, None)))
      val template =
        templateSource.loadTemplate(id, templateString, templateInfo).get

      template.id === id and template.template === templateString and template.parameterInfos("id") === ParameterInfo
        .fromTemplateInfoParameter("id", templateInfo.parameters.get("id").get)
    }

    "require an 'id' parameter (no parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate("test", "Hallo", TemplateInfo(None, Map.empty))
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set()"))
    }

    "require an 'id' parameter (wrong parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate(
          "test",
          "Hallo {{bla}}",
          TemplateInfo(None, Map("bla" -> Parameter(Some("bla"), None, None, ParameterType.Raw, None)))
        )
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        "requirement failed: There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: Set(bla)"))
    }

    "should fail when template info contains a forbidden character" in {
      val tryTemplate =
        templateSource.loadTemplate(
          "test",
          "Hallo {{bla-bla}}",
          TemplateInfo(None,
                       Map("id" -> Parameter(Some("id"), None, None, ParameterType.Raw, None),
                           "bla-bla" -> Parameter(Some("bla"), None, None, ParameterType.Raw, None)))
        )
      (tryTemplate.isFailure must beTrue) and (tryTemplate.failed.get.getMessage must beEqualTo(
        s"requirement failed: Template parameters cannot contain the following characters ${TemplateSource.forbiddenCharacters}"))
    }

    "not expose variables that are not defined in the template config" in {
      val tryTemplate =
        templateSource.loadTemplate(
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
