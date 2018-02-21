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
  private val variablePattern = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9\\-\\_\\_]*)\\}\\}")

  /**
    * Get templates from the source.
    *
    * Does not guarantee to return the same templates on repeated invocations. Can potentially perform I/O
    */
  def loadTemplates(): Seq[Template]

  def loadTemplate(templateId: String,
                   templateString: String,
                   templateInfo: TemplateConfig.TemplateInfo,
                   convertDashesToUnderscores: Boolean): Try[Template] =
    Try {
      // find variables with dashes
      val matcher = variablePattern.matcher(templateString)
      var variables = ArrayBuffer[String]()

      while (matcher.find()) {
        variables += matcher.group(1)
      }
      val variablesWithDashes = variables.toSet.filter(variable => variable.contains("-"))

      val validatedTemplateString = if (convertDashesToUnderscores) {
        variablesWithDashes.foldLeft(templateString) {
          case (template, variable) => {
            val validVariable = variable.replaceAll("-", "_")
            log.warn(s"Converting variable $variable to $validVariable in $templateId")
            template.replaceAll(variable, validVariable)
          }
        }
      } else {
        require(
          variablesWithDashes.isEmpty,
          s"Found variables with dashes: $variablesWithDashes. " +
            s"Please remove the dashes from variable names or set broccoli.templates.convert-dashes-to-underscores to true"
        )
        templateString
      }

      require(
        templateInfo.parameters.contains("id"),
        s"There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: ${templateInfo.parameters.keySet}"
      )

      Template(
        id = templateId,
        template = validatedTemplateString,
        description = templateInfo.description.getOrElse(s"$templateId template"),
        parameterInfos = templateInfo.parameters
          .map { case (id, parameter) => id -> ParameterInfo.fromTemplateInfoParameter(id, parameter) }
      )
    }
}
