package de.frosner.broccoli.models

import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

@volatile
case class Instance(id: String,
                    template: Template,
                    parameterValues: Map[String, String],
                    var status: InstanceStatus,
                    var services: Map[String, Service]) {

  require(template.parameters == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

  val templateJson: JsValue = {
    val replacedTemplate = parameterValues.foldLeft(template.template){
      case (intermediateTemplate, (parameter, value)) => intermediateTemplate.replaceAll("\\{\\{" + parameter + "\\}\\}", value)
    }
    Json.parse(replacedTemplate)
  }

}

object Instance {

  implicit val instanceWrites: Writes[Instance] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "parameterValues").write[Map[String, String]] and
      (JsPath \ "status").write[InstanceStatus] and
      (JsPath \ "services").write[Map[String, Service]]
    )((instance: Instance) => (instance.id, instance.parameterValues, instance.status, instance.services))

}
