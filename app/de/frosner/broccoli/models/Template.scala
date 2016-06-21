package de.frosner.broccoli.models

import java.util.regex.Pattern

import play.api.libs.json._
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

import scala.collection.mutable.ArrayBuffer

case class Template(id: String, template: String, description: String) {

  val parameters: Set[String] = {
    val matcher = Template.TemplatePattern.matcher(template)
    var variables = ArrayBuffer[String]()
    while (matcher.find()) {
      variables += matcher.group(1)
    }
    variables.toSet
  }

}

object Template {

  val TemplatePattern = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9\\_]*)\\}\\}")

  implicit val templateWrites: Writes[Template] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "template").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "parameters").write[Set[String]]
    )((template: Template) => (template.id, template.template, template.description, template.parameters))

  implicit val templateReads: Reads[Template] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "template").read[String] and
      (JsPath \ "description").read[String]
    )(Template.apply _)

}
