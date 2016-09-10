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

  def requireParameterValueConsistency() = {
    val realParametersWithValues = parameterValues.keySet ++ template.parameterInfos.flatMap {
      case (key, ParameterInfo(name, Some(default))) => Some(key)
      case (key, ParameterInfo(name, None)) => None
    }
    require(template.parameters == realParametersWithValues,
      s"The given parameters values (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters}).")
  }

  requireParameterValueConsistency()

  def updateParameterValues(newParameterValues: Map[String, String]): Try[Instance] = {
    Try{
      requireParameterValueConsistency()
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      this.parameterValues = newParameterValues
      this
    }
  }

  def templateJson: JsValue = {
    val templateWithValues = parameterValues.foldLeft(template.template){
      case (intermediateTemplate, (parameter, value)) => intermediateTemplate.replaceAll("\\{\\{" + parameter + "\\}\\}", value)
    }
    val parameterDefaults = template.parameterInfos.flatMap {
      case (parameterId, parameterInfo) => parameterInfo.default.map(default => (parameterId, default))
    }
    val templateWithDefaults = parameterDefaults.foldLeft(templateWithValues){
      case (intermediateTemplate, (parameter, value)) => intermediateTemplate.replaceAll("\\{\\{" + parameter + "\\}\\}", value)
    }
    Json.parse(templateWithDefaults)
  }

}

object Instance {

  implicit val instanceWrites: Writes[Instance] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "parameterValues").write[Map[String, String]] and
      (JsPath \ "status").write[InstanceStatus] and
      (JsPath \ "services").write[Map[String, Service]] and
      (JsPath \ "template").write[Template]
    )((instance: Instance) => (
      instance.id,
      instance.parameterValues,
      instance.status,
      instance.services,
      instance.template
    ))

}
