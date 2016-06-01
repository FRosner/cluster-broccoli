package de.frosner.broccoli.models

import play.api.libs.json.{Json, Reads, Writes}

case class Instance(id: String, template: Template, parameterValues: Map[String, String]) {

  require(template.parameters == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

}

object Instance {

  val instances = Seq(Instance("1", Template.templates.head, Map("name" -> "Frank")))

  implicit val instanceWrites = Json.writes[Instance]

  implicit val instanceReads = Json.reads[Instance]

}
