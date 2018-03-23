package de.frosner.broccoli.templates

import javax.inject.Inject

import de.frosner.broccoli.models.{Instance, ParameterInfo, ParameterType}
import play.api.libs.json.{JsString, JsValue, Json}

/**
  * Renders json representation of the passed instance
  *
  * @param defaultType The default type of template parameters
  */
class TemplateRenderer(defaultType: ParameterType) {

  def renderJson(instance: Instance): JsValue = {
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val parameterValues = instance.parameterValues

    val templateWithValues = parameterValues.foldLeft(template.template) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", value.asJsonString)
    }
    val parameterDefaults = parameterInfos.flatMap {
      case (parameterId, parameterInfo) => parameterInfo.default.map(default => (parameterId, default))
    }
    val templateWithDefaults = parameterDefaults.foldLeft(templateWithValues) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", value.asJsonString)
    }
    Json.parse(templateWithDefaults)
  }
}
