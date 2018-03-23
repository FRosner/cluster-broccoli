package de.frosner.broccoli.models

import de.frosner.broccoli.templates.TemplateConfig
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

// TODO add display name and description also
case class ParameterInfo(id: String,
                         name: Option[String],
                         default: Option[ParameterValue],
                         secret: Option[Boolean],
                         `type`: Option[ParameterType],
                         orderIndex: Option[Int])

object ParameterInfo {

  implicit val parameterInfoWrites = Json.writes[ParameterInfo]

  implicit val parameterInfoReads = new Reads[ParameterInfo] {
    override def reads(json: JsValue): JsResult[ParameterInfo] = {
      Try {
        val id = (json \ "id").as[String]
        val name = (json \ "id").asOpt[String]
        val secret = (json \ "id").asOpt[Boolean]
        val `type` =  (json \ "type").asOpt[String].map(ParameterType.withName)
        val orderIndex = (json \ "orderIndex").asOpt[Int]
        val default = (`type`, (json \ "default").toOption) match {
          case (Some(paramType), Some(jsValue)) =>
            ParameterValue.constructParameterValueFromJson(paramType, jsValue)
          case _ => None
        }
        ParameterInfo(id, name, default, secret, `type`, orderIndex)
      } match {
        case Success(parameterInfo) => JsSuccess(parameterInfo)
        case Failure(ex) => JsError(ex.getMessage)
      }

    }
  }

  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
