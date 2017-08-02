package de.frosner.broccoli.templates

import de.frosner.broccoli.models.{Instance, ParameterType}
import play.api.libs.json.{JsString, JsValue, Json}

class TemplateRenderer {
  def templateJson(instance: Instance): JsValue = {
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val parameterValues = instance.parameterValues

    def sanitize(parameter: String, value: String) = {
      val parameterType = parameterInfos.get(parameter).flatMap(_.`type`).getOrElse(ParameterType.Raw) // TODO get this from config
      val sanitized = parameterType match {
        case ParameterType.Raw    => value
        case ParameterType.String => JsString(value)
      }
      sanitized.toString
    }

    val templateWithValues = parameterValues.foldLeft(template.template) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", sanitize(parameter, value))
    }
    val parameterDefaults = parameterInfos.flatMap {
      case (parameterId, parameterInfo) => parameterInfo.default.map(default => (parameterId, default))
    }
    val templateWithDefaults = parameterDefaults.foldLeft(templateWithValues) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", sanitize(parameter, value))
    }
    Json.parse(templateWithDefaults)
  }
}
