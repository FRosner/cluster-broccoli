package de.frosner.broccoli.models

import de.frosner.broccoli.templates.TemplateConfig
import play.api.libs.json.Json

// TODO add display name and description also
case class ParameterInfo(id: String,
                         name: Option[String],
                         default: Option[String],
                         secret: Option[Boolean],
                         `type`: Option[ParameterType],
                         orderIndex: Option[Int])

object ParameterInfo {

  implicit val parameterInfoWrites = Json.writes[ParameterInfo]

  implicit val parameterInfoReads = Json.reads[ParameterInfo]

  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
