package de.frosner.broccoli.models

import play.api.libs.json.Json

// TODO add display name and description also
case class ParameterInfo(id: String, name: Option[String], default: Option[String], secret: Option[Boolean])

object ParameterInfo {

  implicit val parameterInfoWrites = Json.writes[ParameterInfo]

  implicit val parameterInfoReads = Json.reads[ParameterInfo]

}
