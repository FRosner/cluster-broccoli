package de.frosner.broccoli.templates

import java.util.regex.Pattern

import com.hubspot.jinjava.Jinjava
import de.frosner.broccoli.models.{ParameterInfo, Template}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import scala.collection.JavaConversions._

/**
  * Provide a source of templates to create an instances from
  */
trait TemplateSource {
  private val log = play.api.Logger(getClass)

  /**
    * Get templates from the source.
    *
    * Does not guarantee to return the same templates on repeated invocations. Can potentially perform I/O
    */
  def loadTemplates(): Seq[Template]

  def loadTemplate(templateId: String,
                   templateString: String,
                   templateInfo: TemplateConfig.TemplateInfo): Try[Template] =
    Try {

      require(
        templateInfo.parameters.contains("id"),
        s"There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: ${templateInfo.parameters.keySet}"
      )

      Template(
        id = templateId,
        template = templateString,
        description = templateInfo.description.getOrElse(s"$templateId template"),
        parameterInfos = templateInfo.parameters
          .map { case (id, parameter) => id -> ParameterInfo.fromTemplateInfoParameter(id, parameter) }
      )
    }
}
