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
      val templateInfo = TemplateInfo(None, Some(Map("id" -> Parameter(Some("id"), None, None, None, None))))
      val convertDashesToUnderscores = false
      val template =
        templateSource.loadTemplate(id, templateString, templateInfo, convertDashesToUnderscores).get
      template.id === id and template.template === templateString and template.parameterInfos("id") === ParameterInfo
        .fromTemplateInfoParameter("id", templateInfo.parameters.get("id"))
    }

    "fail if the 'legacy' variables contain dashes and convertDashesToUnderscores is false" in {
      val tryTemplate =
        templateSource.loadTemplate("templateId", "Hello {{ id}} {{custom-var}}", TemplateInfo(None, None), false)
      tryTemplate.isFailure
    }

    "convert dashes in 'legacy' variables to underscores if convertDashesToUnderscores is true" in {
      val tryTemplate =
        templateSource.loadTemplate("templateId", "Hello {{id}} {{custom-var}}", TemplateInfo(None, None), true)
      tryTemplate.isSuccess and tryTemplate.get.parameters.contains("custom_var")
    }

    "require an 'id' parameter (no parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate("test", "Hallo", TemplateInfo(None, None), false)
      tryTemplate.isFailure
    }

    "require an 'id' parameter (wrong parameter)" in {
      val tryTemplate =
        templateSource.loadTemplate("test", "Hallo {{bla}}", TemplateInfo(None, None), false)
      tryTemplate.isFailure
    }
  }
}
