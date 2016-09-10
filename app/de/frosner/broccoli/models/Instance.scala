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

  require(template.parameterNames == parameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
    s"need to match the ones in the template (${template.parameters}).")

  def updateParameterValues(newParameterValues: Map[String, String]): Try[Instance] = {
    Try{
      require(template.parameterNames == newParameterValues.keySet, s"The given parameters (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters}).")
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      this.parameterValues = newParameterValues
      this
    }
  }

  def templateJson: JsValue = {
    val (replacedTemplate, finalOffset) = template.parameters.foldLeft((template.template, 0)){
      case ((intermediateTemplate, offset), parameter) =>
        val parameterValue = parameterValues(parameter.name)
        val newIntermediateTemplate = intermediateTemplate.substring(0, parameter.start + offset) +
          parameterValue +
          intermediateTemplate.substring(parameter.end + offset)
        val newOffset = offset + parameterValue.length - (parameter.end - parameter.start)
        (newIntermediateTemplate, newOffset)
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
      (JsPath \ "template" \ "version").write[String] and
      (JsPath \ "template" \ "id").write[String]
    )((instance: Instance) => (
      instance.id,
      instance.parameterValues,
      instance.status,
      instance.services,
      instance.template.templateVersion,
      instance.template.id
    ))

}
