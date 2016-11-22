package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

case class Instance(id: String,
                    template: Template,
                    parameterValues: Map[String, String]) extends Serializable {

  def requireParameterValueConsistency(parameterValues: Map[String, String], template: Template) = {
    val realParametersWithValues = parameterValues.keySet ++ template.parameterInfos.flatMap {
      case (key, ParameterInfo(name, Some(default), _)) => Some(key)
      case (key, ParameterInfo(name, None, _)) => None
    }
    require(template.parameters == realParametersWithValues,
      s"The given parameters values (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters}).")
  }

  requireParameterValueConsistency(parameterValues, template)

  def updateParameterValues(newParameterValues: Map[String, String]): Try[Instance] = {
    Try{
      requireParameterValueConsistency(newParameterValues, template)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = this.template,
        parameterValues = newParameterValues
      )
    }
  }

  def updateTemplate(newTemplate: Template, newParameterValues: Map[String, String]): Try[Instance] = {
    Try{
      requireParameterValueConsistency(newParameterValues, newTemplate)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = newTemplate,
        parameterValues = newParameterValues
      )
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

  implicit val instanceApiWrites: Writes[Instance] = {
    import JobStatusJson.instanceStatusWrites
    import Template.templateApiWrites
    Json.writes[Instance]
  }

  implicit val instancePersistenceWrites: Writes[Instance] = {
    import JobStatusJson.instanceStatusWrites
    import Template.templatePersistenceWrites
    Json.writes[Instance]
  }

  implicit val instancePersistenceReads: Reads[Instance] = {
    import Template.templatePersistenceReads
    import JobStatusJson.instanceStatusReads
    Json.reads[Instance]
  }

}
