package de.frosner.broccoli.models

import play.api.libs.json.Json

case class Instance(id: String, template: Template, parameterValues: Map[String, String]) {

  require(template.parameters == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

}

object Instance {

  implicit val instanceWrites = Json.writes[Instance]

  implicit val instanceReads = Json.reads[Instance]

}
