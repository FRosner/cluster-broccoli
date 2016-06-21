package de.frosner.broccoli.models

import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import play.api.libs.json.{JsValue, Json}

@volatile
case class Instance(id: String,
                    template: Template,
                    parameterValues: Map[String, String],
                    var status: InstanceStatus,
                    var services: Map[String, Service]) {

  require(template.parameters == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

  // TODO inject parameter values
  // TODO avoid JSON injection (escape the shit out of it)
  val templateJson: JsValue = {
    val replacedTemplate = parameterValues.foldLeft(template.template){
      case (intermediateTemplate, (parameter, value)) => intermediateTemplate.replaceAll("\\$\\{" + parameter + "\\}", value)
    }
    Json.parse(replacedTemplate)
  }

}

object Instance {

  implicit val instanceWrites = Json.writes[Instance]

  implicit val instanceReads = Json.reads[Instance]

}
