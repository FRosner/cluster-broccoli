package de.frosner.broccoli.templates


import de.frosner.broccoli.models.Instance
import play.api.libs.json.{JsValue, Json}

/**
  * Renders json representation of the passed instance
  */
object TemplateRenderer {

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
