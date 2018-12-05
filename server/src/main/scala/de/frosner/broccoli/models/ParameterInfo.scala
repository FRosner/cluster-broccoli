package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.ParameterType.paramTypeFromString
import de.frosner.broccoli.models.SetProvider.{StaticDoubleSetProvider, StaticIntSetProvider, StaticStringSetProvider, UserOESetProvider}
import de.frosner.broccoli.services.ParameterTypeException
import de.frosner.broccoli.templates.TemplateConfig
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

// TODO add display name and description also
case class ParameterInfo(id: String,
                         name: Option[String],
                         default: Option[ParameterValue],
                         secret: Option[Boolean],
                         `type`: ParameterType,
                         orderIndex: Option[Int])

object ParameterInfo {

  // implicit val parameterInfoWrites: Writes[ParameterInfo] = Json.writes[ParameterInfo]

  implicit val parameterInfoWrites: Writes[ParameterInfo] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "default").writeNullable[ParameterValue] and
      (JsPath \ "secret").writeNullable[Boolean] and
      (JsPath \ "type").write[ParameterType](ParameterType.parameterTypeWrites) and
      (JsPath \ "orderIndex").writeNullable[Int]
  )(unlift(ParameterInfo.unapply))

  implicit def parameterInfoApiWrites(implicit account: Account): Writes[ParameterInfo] =
    (
      (JsPath \ "id").write[String] and
        (JsPath \ "name").writeNullable[String] and
        (JsPath \ "default").writeNullable[ParameterValue] and
        (JsPath \ "secret").writeNullable[Boolean] and
        (JsPath \ "type").write[ParameterType](ParameterType.parameterTypeApiWrites) and
        (JsPath \ "orderIndex").writeNullable[Int]
    )(unlift(ParameterInfo.unapply))

  implicit val parameterInfoReads: Reads[ParameterInfo] = new Reads[ParameterInfo] {
    override def reads(json: JsValue): JsResult[ParameterInfo] =
      Try {
        val id = (json \ "id").as[String]
        val name = (json \ "name").asOpt[String]
        val secret = (json \ "secret").asOpt[Boolean]
        val `type` = typeFromJson(id, (json \ "type").as[JsValue])
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

  def typeFromJson(id: String, json: JsValue): ParameterType =
    json match {
      case JsString(value) =>
        // TODO: This section is for older compatibility. Remove it in the future
        paramTypeFromString(value).getOrElse(throw ParameterTypeException(s"Unsupported data type for parameter $id"))
      case JsObject(underlying) =>
        underlying.getOrElse("name", JsNull) match {
          case JsString(name) =>
            name match {
              case "raw"     => ParameterType.Raw
              case "string"  => ParameterType.String
              case "integer" => ParameterType.Integer
              case "decimal" => ParameterType.String
              case "set" =>
                val metadata =
                  underlying.getOrElse(
                    "metadata",
                    throw ParameterTypeException(s"set type must have metadata in param $id")
                  )
                // TODO: We could use reflection here so user provided implementations also work
                (metadata \ "provider").as[String] match {
                  case "StaticIntSetProvider" =>
                    ParameterType.Set(StaticIntSetProvider((metadata \ "values").as[Set[Int]]))
                  case "StaticDoubleSetProvider" =>
                    ParameterType.Set(StaticDoubleSetProvider((metadata \ "values").as[Set[Double]]))
                  case "StaticStringSetProvider" =>
                    ParameterType.Set(StaticStringSetProvider((metadata \ "values").as[Set[String]]))
                  case "UserOESetProvider" =>
                    ParameterType.Set(UserOESetProvider)
                  case _ =>
                    throw ParameterTypeException(s"Unsupported provider class in metadata for parameter `$id`")
                }
            }
          case _ => throw ParameterTypeException(s"Invalid json for key `name` in metadata for parameter `$id`")
        }
      case _ => throw ParameterTypeException(s"Could not parse `type` for parameter `$id`")
    }

  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
