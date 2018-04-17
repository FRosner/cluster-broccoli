package de.frosner.broccoli.models

import play.api.libs.json.{JsValue, Json}

case class InstanceCreation(templateId: String, parameters: Map[String, JsValue])

object InstanceCreation {

  implicit val instanceCreationWrites = Json.writes[InstanceCreation]

  implicit val instanceCreationReads = Json.reads[InstanceCreation]

}
