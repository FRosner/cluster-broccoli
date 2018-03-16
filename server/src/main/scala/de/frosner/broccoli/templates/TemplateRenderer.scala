package de.frosner.broccoli.templates

import javax.inject.Inject

import de.frosner.broccoli.instances.InstanceConfiguration
import de.frosner.broccoli.models.{Instance, ParameterInfo, ParameterType}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json.{JsString, JsValue, Json}

import scala.util.{Failure, Success, Try}

/**
  * Renders json representation of the passed instance
  *
  * @param defaultType The default type of template parameters
  */
class TemplateRenderer(defaultType: ParameterType) {

  // Checks if the value is a number
  private def isNumber(value: String): Boolean = {
    val expr = "^-?[0-9]+(\\.[0-9]+)?$".r
    value match {
      case expr(_) =>
        true
      case _ =>
        false
    }
  }

  def sanitize(parameter: String, value: String, parameterInfos: Map[String, ParameterInfo]): String = {
    val parameterType = parameterInfos
      .get(parameter)
      .flatMap(_.`type`)
      .getOrElse(defaultType)
    val sanitized = parameterType match {
      case ParameterType.Raw => value
      case ParameterType.String =>
        StringEscapeUtils.escapeJson(value)
      case ParameterType.Integer =>
        Try(value.toInt) match {
          case Success(num) =>
            num.toString
          case Failure(_) =>
            throw new IllegalArgumentException(s"expected an integer for {$parameter}. Found {$value}")
        }
      case ParameterType.Float =>
        Try(value.toFloat) match {
          case Success(num) =>
            num.toString
          case Failure(_) =>
            throw new IllegalArgumentException(s"expected an integer for {$parameter}. Found {$value}")
        }
    }
    sanitized
  }

  def renderJson(instance: Instance): JsValue = {
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val parameterValues = instance.parameterValues

    val templateWithValues = parameterValues.foldLeft(template.template) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", sanitize(parameter, value, parameterInfos))
    }
    val parameterDefaults = parameterInfos.flatMap {
      case (parameterId, parameterInfo) => parameterInfo.default.map(default => (parameterId, default))
    }
    val templateWithDefaults = parameterDefaults.foldLeft(templateWithValues) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", sanitize(parameter, value, parameterInfos))
    }
    Json.parse(templateWithDefaults)
  }
}
