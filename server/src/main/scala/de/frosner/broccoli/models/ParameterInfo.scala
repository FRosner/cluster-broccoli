package de.frosner.broccoli.models

import de.frosner.broccoli.templates.TemplateConfig
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

// TODO add display name and description also
case class ParameterInfo(id: String,
                         name: Option[String],
                         default: Option[ParameterValue],
                         secret: Option[Boolean],
                         `type`: ParameterType,
                         orderIndex: Option[Int])

object ParameterInfo {

  implicit val parameterInfoWrites = Json.writes[ParameterInfo]

  implicit val parameterInfoReads = new Reads[ParameterInfo] {
    override def reads(json: JsValue): JsResult[ParameterInfo] =
      Try {
        val id = (json \ "id").as[String]
        val name = (json \ "name").asOpt[String]
        val secret = (json \ "secret").asOpt[Boolean]
        val `type` = ParameterType.withName((json \ "type").as[String])
        val orderIndex = (json \ "orderIndex").asOpt[Int]
        val default = (`type`, (json \ "default").toOption) match {
          case (paramType, Some(jsValue)) =>
            ParameterValue.fromJsValue(paramType, jsValue).toOption
          case _ => None
        }
        ParameterInfo(id, name, default, secret, `type`, orderIndex)
      } match {
        case Success(parameterInfo) => JsSuccess(parameterInfo)
        case Failure(ex)            => JsError(ex.getMessage)
      }

  }

  // TODO: Pick the default from the reference.conf and use it for parameter.`type`
  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
