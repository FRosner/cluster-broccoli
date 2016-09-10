package de.frosner.broccoli.models

import java.util.regex.Pattern

import org.apache.commons.codec.digest.DigestUtils
import play.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import Parameter.parameterWrites

import scala.collection.mutable.ArrayBuffer

case class Template(id: String, template: String, description: String) extends Serializable {

  @transient
  lazy val parameters: Seq[Parameter] = {
    val parameterMatcher = Template.ParameterPattern.matcher(template)
    var variables = ArrayBuffer[Parameter]()
    def isAdvancedSyntax(parameter: String): Boolean = parameter.contains(":")
    while (parameterMatcher.find()) {
      val parameter = parameterMatcher.group(1).stripPrefix("{{").stripSuffix("}}").trim
      val start = parameterMatcher.start(1)
      val end =  parameterMatcher.end(1)
      if (isAdvancedSyntax(parameter)) {
        val maybeParameter = ParameterParser(parameter, start, end)
        if (maybeParameter.isEmpty) {
          Logger.warn(s"Ignoring parameter '$parameter' with invalid syntax.")
        } else {
          variables += maybeParameter.get
        }
      } else if (parameter.matches(s"(${Template.NameSyntax})")) {
        variables += Parameter(parameter, start, end)
      } else {
        Logger.warn(s"Ignoring parameter '$parameter' because the name is invalid.")
      }
    }
    val uniqueParameterNames = variables.map(_.name).toSet
    require(uniqueParameterNames.contains("id"),
      s"There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: $uniqueParameterNames")
    variables.sortBy(_.start)
  }

  @transient
  lazy val parameterNames: Set[String] = parameters.map(_.name).toSet

  @transient
  lazy val templateVersion: String = DigestUtils.md5Hex(template)

}

object Template {

  val NameSyntax = "[A-Za-z][A-Za-z0-9\\-\\_\\_]*"
  val ParameterPattern = Pattern.compile("(\\{\\{.+?\\}\\})")
  val ParameterNamePattern = Pattern.compile(s"name:($NameSyntax)")

  implicit val templateWrites: Writes[Template] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "parameterNames").write[Set[String]] and
      (JsPath \ "version").write[String]
    )((template: Template) => (template.id, template.description, template.parameterNames, template.templateVersion))

}
