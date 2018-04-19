package de.frosner.broccoli.models

import com.typesafe.config.{Config, ConfigValue, ConfigValueType}
import com.typesafe.config.impl.ConfigInt
import de.frosner.broccoli.services.{ParameterNotFoundException, ParameterValueParsingException}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object ParameterValue {

  implicit val parameterValueWrites: Writes[ParameterValue] = new Writes[ParameterValue] {
    override def writes(paramValue: ParameterValue) = paramValue.asJsValue
  }

  def fromConfigValue(paramName: String, parameterType: ParameterType, configValue: ConfigValue): Try[ParameterValue] =
    Try {
      parameterType match {
        case ParameterType.Raw =>
          RawParameterValue(configValue.unwrapped().toString)
        case ParameterType.String =>
          StringParameterValue(configValue.unwrapped.asInstanceOf[String])
        case ParameterType.Integer =>
          val number = configValue.unwrapped().asInstanceOf[Number]
          //noinspection ComparingUnrelatedTypes
          if (number == number.intValue())
            IntParameterValue(number.intValue())
          else
            throw ParameterValueParsingException(paramName, s"${number.toString} is not a valid integer")
        case ParameterType.Decimal =>
          DecimalParameterValue(BigDecimal(configValue.unwrapped().asInstanceOf[Number].toString))
      }
    }

  def fromJsValue(parameterName: String,
                  parameterInfos: Map[String, ParameterInfo],
                  jsValue: JsValue): Try[ParameterValue] =
    Try {
      val parameterInfo =
        parameterInfos.getOrElse(parameterName, throw ParameterNotFoundException(parameterName, parameterInfos.keySet))
      fromJsValue(parameterInfo.`type`, jsValue) match {
        case Success(param) => param
        case Failure(ex) =>
          throw ParameterValueParsingException(parameterName, ex.getMessage)
      }
    }

  def fromJsValue(parameterType: ParameterType, jsValue: JsValue): Try[ParameterValue] =
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
    }
}
sealed trait ParameterValue {

  /**
    * Implemented by each parameter value depending on what it wants
    * @return String representation for the parameter value which is JSON deserializable
    */
  def asJsonString: String
  def asJsValue: JsValue
}

case class IntParameterValue(value: Int) extends ParameterValue {
  override def asJsonString: String = value.toString
  override def asJsValue: JsValue = JsNumber(value)
}
case class DecimalParameterValue(value: BigDecimal) extends ParameterValue {
  override def asJsonString: String = value.toString
  override def asJsValue: JsValue = JsNumber(value)
}
case class StringParameterValue(value: String) extends ParameterValue {
  override def asJsonString: String = StringEscapeUtils.escapeJson(value)
  override def asJsValue: JsValue = JsString(value)
}
case class RawParameterValue(value: String) extends ParameterValue {
  override def asJsonString: String = value
  override def asJsValue: JsValue = JsString(value)
}
