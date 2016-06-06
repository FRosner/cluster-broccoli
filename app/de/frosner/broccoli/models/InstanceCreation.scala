package de.frosner.broccoli.models

import play.api.libs.json.Json

case class InstanceCreation(templateId: String, parameters: Map[String, String])

object InstanceCreation {

  implicit val instanceCreationWrites = Json.writes[InstanceCreation]

  implicit val instanceCreationReads = Json.reads[InstanceCreation]

}
