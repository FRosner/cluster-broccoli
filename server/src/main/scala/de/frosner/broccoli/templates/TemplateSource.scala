package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{ParameterInfo, Template, TemplateFormat}

import scala.util.Try

trait TemplateSource {
  private val _ = play.api.Logger(getClass)

  val templateRenderer: TemplateRenderer

  /**
    * Get templates from the source.
    *
    * Does not guarantee to return the same templates on repeated invocations. Can potentially perform I/O
    */
  def loadTemplates(): Seq[Template] = loadTemplates(false)

  def loadTemplates(refreshed: Boolean): Seq[Template]

  def loadTemplate(templateId: String,
                   templateString: String,
                   templateInfo: TemplateConfig.TemplateInfo,
                   templateFormat: TemplateFormat = TemplateFormat.JSON): Try[Template] =
    Try {

      require(
        templateInfo.parameters.contains("id"),
        s"There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: ${templateInfo.parameters.keySet}"
      )

      val invalidParamNames = templateInfo.parameters.keys.filterNot(templateRenderer.validateParameterName)

      require(
        invalidParamNames.isEmpty,
        s"""The following parameters are invalid: ${invalidParamNames.mkString(", ")}"""
      )

      Template(
        id = templateId,
        template = templateString,
        format = templateFormat,
        description = templateInfo.description.getOrElse(s"$templateId template"),
        parameterInfos = templateInfo.parameters
          .map { case (id, parameter) => id -> ParameterInfo.fromTemplateInfoParameter(id, parameter) }
      )
    }
}
