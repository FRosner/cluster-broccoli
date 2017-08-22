package de.frosner.broccoli.models

import play.api.libs.json.Json

// TODO add display name and description also
case class ParameterInfo(id: String,
                         name: Option[String],
                         default: Option[String],
                         secret: Option[Boolean],
                         `type`: Option[ParameterType])

object ParameterInfo {

  implicit val parameterInfoWrites = Json.writes[ParameterInfo]

  implicit val parameterInfoReads = Json.reads[ParameterInfo]

  def fromMetaParameter(id: String, parameter: Meta.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`)
}
