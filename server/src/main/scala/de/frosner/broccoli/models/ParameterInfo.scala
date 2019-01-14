package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.ListProvider.{
  StaticDoubleListProvider,
  StaticIntListProvider,
  StaticStringListProvider
}
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
        val `type` = ParameterType.fromJson(id, (json \ "type").as[JsValue])
        val orderIndex = (json \ "orderIndex").asOpt[Int]
        val default = (`type`, (json \ "default").toOption) match {
          case (paramType, Some(jsValue)) =>
            ParameterValue.fromJsValue(paramType, jsValue).toOption
          case (paramType, None) =>
            paramType match {
              case ParameterType.List(provider) =>
                provider match {
                  case StaticIntListProvider(values) =>
                    Some(IntParameterValue(values.head))
                  case StaticDoubleListProvider(values) =>
                    Some(DecimalParameterValue(values.head))
                  case StaticStringListProvider(values) =>
                    Some(StringParameterValue(values.head))
                  case _ => None
                }
              case _ => None
            }
          case _ => None
        }
        ParameterInfo(id, name, default, secret, `type`, orderIndex)
      } match {
        case Success(parameterInfo) => JsSuccess(parameterInfo)
        case Failure(ex)            => JsError(ex.getMessage)
      }
  }

  def fromTemplateInfoParameter(id: String, parameter: TemplateConfig.Parameter): ParameterInfo =
    ParameterInfo(id, parameter.name, parameter.default, parameter.secret, parameter.`type`, parameter.orderIndex)
}
