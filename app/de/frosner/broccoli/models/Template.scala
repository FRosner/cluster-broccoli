package de.frosner.broccoli.models

import java.util.regex.Pattern

import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.mutable.ArrayBuffer

case class Template(id: String, template: String, description: String) extends Serializable {

  @transient
  lazy val parameters: Set[String] = {
    val matcher = Template.TemplatePattern.matcher(template)
    var variables = ArrayBuffer[String]()
    while (matcher.find()) {
      variables += matcher.group(1)
    }
    val uniqueVariables = variables.toSet
    require(uniqueVariables.contains("id"),
      s"There needs to be an 'id' field in the template for Broccoli to work. Parameters defined: ${uniqueVariables}")
    uniqueVariables
  }

  @transient
  lazy val templateVersion: String = DigestUtils.md5Hex(template)

}

object Template {

  val TemplatePattern = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9\\-\\_\\_]*)\\}\\}")

  implicit val templateWrites: Writes[Template] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "parameters").write[Set[String]] and
      (JsPath \ "version").write[String]
    )((template: Template) => (template.id, template.description, template.parameters, template.templateVersion))

}
