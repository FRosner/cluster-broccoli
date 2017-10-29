package de.frosner.broccoli.templates


import com.hubspot.jinjava.{Jinjava, JinjavaConfig}
import de.frosner.broccoli.models.Instance
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConversions._

/**
  * Renders json representation of the passed instance
  * @param jinjavaConfig Jinjava configuration
  */
class TemplateRenderer(jinjavaConfig: JinjavaConfig) {
  val jinjava = new Jinjava(jinjavaConfig)

  def renderJson(instance: Instance): JsValue = {
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val parameterDefaults = parameterInfos
      .map {
        case (name, parameterInfo) => (name, parameterInfo.default)
      }
      .collect {
        case (name, Some(value)) => (name, value)
      }
    val parameterValues = (parameterDefaults ++ instance.parameterValues).map {
      case (name, value) => (name, value.asJsonString)
    }
    Json.parse(jinjava.render(template.template, parameterValues))
  }
}
