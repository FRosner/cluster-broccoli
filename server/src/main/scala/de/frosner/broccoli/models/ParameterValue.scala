package de.frosner.broccoli.models

import com.typesafe.config.{Config, ConfigValue, ConfigValueType}
import com.typesafe.config.impl.ConfigInt
import de.frosner.broccoli.services.{ParameterValueParsingException, TemplateParameterNotFoundException}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object ParameterValue {

  implicit val parameterValueWrites: Writes[ParameterValue] = new Writes[ParameterValue] {
    override def writes(paramValue: ParameterValue) = paramValue.asJsValue
  }

  def constructParameterValueFromTypesafeConfig(parameterType: ParameterType,
                                                configValue: ConfigValue): Try[ParameterValue] =
    Try {
      parameterType match {
        case ParameterType.Raw =>
          RawParameterValue(configValue.unwrapped().toString)
        case ParameterType.String =>
          StringParameterValue(configValue.unwrapped.asInstanceOf[String])
        case ParameterType.Integer =>
          IntParameterValue(configValue.unwrapped().asInstanceOf[Int])
        case ParameterType.Decimal =>
          DecimalParameterValue(configValue.unwrapped().asInstanceOf[BigDecimal])
      }
    }

  def constructParameterValueFromJson(parameterName: String,
                                      template: Template,
                                      jsValue: JsValue): Try[ParameterValue] =
    Try {
      val parameterInfo =
        template.parameterInfos.getOrElse(parameterName, ParameterInfo(parameterName, None, None, None, None, None))
      constructParameterValueFromJson(parameterInfo.`type`.getOrElse(ParameterType.Raw), jsValue) match {
        case Some(param) => param
        case None =>
          throw ParameterValueParsingException(template.id, parameterName)
      }
    }

  def constructParameterValueFromJson(parameterType: ParameterType, jsValue: JsValue): Option[ParameterValue] =
    Try {
      parameterType match {
        case ParameterType.Raw =>
          RawParameterValue(jsValue.as[String])
        case ParameterType.String =>
          StringParameterValue(jsValue.as[String])
        case ParameterType.Integer =>
          IntParameterValue(jsValue.as[Int])
        case ParameterType.Decimal =>
          DecimalParameterValue(jsValue.as[BigDecimal])
      }
    } match {
      case Success(s) => Some(s)
      case Failure(_) => None
    }
}
sealed trait ParameterValue {
  def isEmpty: Boolean

  /**
    * Implemented by each parameter value depending on what it wants
    * @return String representation for the parameter value which is JSON deserializable
    */
  def asJsonString: String
  def asJsValue: JsValue
}

case class IntParameterValue(value: Int) extends ParameterValue {
  override def isEmpty: Boolean = false // can never be empty
  override def asJsonString: String = value.toString
  override def asJsValue: JsValue = JsNumber(value)
}
case class DecimalParameterValue(value: BigDecimal) extends ParameterValue {
  override def isEmpty: Boolean = false // can never be empty
  override def asJsonString: String = value.toString
  override def asJsValue: JsValue = JsNumber(value)
}
case class StringParameterValue(value: String) extends ParameterValue {
  override def isEmpty: Boolean = value.isEmpty
  override def asJsonString: String = StringEscapeUtils.escapeJson(value)
  override def asJsValue: JsValue = JsString(value)
}
case class RawParameterValue(value: String) extends ParameterValue {
  override def isEmpty: Boolean = value.isEmpty
  override def asJsonString: String = value
  override def asJsValue: JsValue = JsString(value)
}
