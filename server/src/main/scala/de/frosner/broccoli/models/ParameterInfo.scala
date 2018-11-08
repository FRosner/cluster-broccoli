package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.services.{ParameterMetadataException}
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

  def parameterInfoReads(json: JsValue, account: Account): Reads[ParameterInfo] = {
    json: JsValue => {
      Try {
        val id = (json \ "id").as[String]
        val name = (json \ "name").asOpt[String]
        val secret = (json \ "secret").asOpt[Boolean]
        val `type` = typeFromJson((json \ "type").as[JsValue], account)
        val orderIndex = (json \ "orderIndex").asOpt[Int]
        val default = (`type`, (json \ "default").toOption) match {
          case (paramType, Some(jsValue)) =>
            ParameterValue.fromJsValue(paramType, jsValue).toOption
          case _ => None
        }
        ParameterInfo(id, name, default, secret, `type`, orderIndex)
      } match {
        case Success(parameterInfo) => JsSuccess(parameterInfo)
        case Failure(ex) => JsError(ex.getMessage)
      }
    }
  }

  def typeFromJson(json: JsValue, account: Account): ParameterType = {
    (json \ "name").as[String].toLowerCase match {
      case "raw" => ParameterType.Raw
      case "string" => ParameterType.String
      case "integer" => ParameterType.Integer
      case "decimal" => ParameterType.String
      case "set" =>
        val metadata = (json \ "metadata").as[JsValue]
        // TODO: We could use reflection here so user provided implementations also work
        (metadata \ "provider").as[String] match {
          case "StaticSetProvider" =>
            (metadata \ "innerType").as[String].toLowerCase match {
              case "integer" =>
                ParameterType.Set[Int](StaticSetProvider.fromJson[Int](json))
              case "decimal" =>
                ParameterType.Set[Double](StaticSetProvider.fromJson[Double](json))
              case "string" =>
                ParameterType.Set[String](StaticSetProvider.fromJson[String](json))
              case _ => throw ParameterMetadataException("inner type must be one of integer|decimal|string")
            }
          case "UserOESetProvider" =>
            ParameterType.Set[String](UserOESetProvider(account))
          case _ =>
            throw ParameterMetadataException("Unsupported provider class")
        }
    }
  }

  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
