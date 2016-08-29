package de.frosner.broccoli.models

import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

case class Instance(id: String,
                    template: Template,
                    var parameterValues: Map[String, String],
                    var status: InstanceStatus,
                    var services: Map[String, Service]) extends Serializable {

  require(template.parameters == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

  def updateParameterValues(newParameterValues: Map[String, String]): Try[Instance] = {
    Try{
      require(template.parameters == newParameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters}).")
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      this.parameterValues = newParameterValues
      this
    }
  }

  def templateJson: JsValue = {
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
      (JsPath \ "services").write[Map[String, Service]] and
      (JsPath \ "template" \ "version").write[String]
    )((instance: Instance) => (instance.id, instance.parameterValues, instance.status, instance.services, instance.template.templateVersion))

}
