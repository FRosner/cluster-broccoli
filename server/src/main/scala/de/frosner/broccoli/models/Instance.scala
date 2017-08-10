package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets
import play.api.libs.json._

import scala.util.Try

case class Instance(id: String, template: Template, parameterValues: Map[String, String]) extends Serializable {

  def requireParameterValueConsistency(parameterValues: Map[String, String], template: Template) = {
    val realParametersWithValues = parameterValues.keySet ++ template.parameterInfos.flatMap {
      case (key, ParameterInfo(_, _, Some(default), _)) => Some(key)
      case (key, ParameterInfo(_, _, None, _))          => None
    }
    require(
      template.parameters == realParametersWithValues,
      s"The given parameters values (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters})."
    )
  }

  requireParameterValueConsistency(parameterValues, template)

  def updateParameterValues(newParameterValues: Map[String, String]): Try[Instance] =
    Try {
      requireParameterValueConsistency(newParameterValues, template)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = this.template,
        parameterValues = newParameterValues
      )
    }

  def updateTemplate(newTemplate: Template, newParameterValues: Map[String, String]): Try[Instance] =
    Try {
      requireParameterValueConsistency(newParameterValues, newTemplate)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = newTemplate,
        parameterValues = newParameterValues
      )
    }

  def templateJson: JsValue = {
    val templateWithValues = parameterValues.foldLeft(template.template) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", value)
    }
    val parameterDefaults = template.parameterInfos.flatMap {
      case (parameterId, parameterInfo) => parameterInfo.default.map(default => (parameterId, default))
    }
    val templateWithDefaults = parameterDefaults.foldLeft(templateWithValues) {
      case (intermediateTemplate, (parameter, value)) =>
        intermediateTemplate.replaceAllLiterally(s"{{$parameter}}", value)
    }
    Json.parse(templateWithDefaults)
  }

}

object Instance {
  implicit val instanceApiWrites: Writes[Instance] = {
    import Template.templateApiWrites
    Json.writes[Instance]
  }

  implicit val instancePersistenceWrites: Writes[Instance] = {
    import Template.templatePersistenceWrites
    Json.writes[Instance]
  }

  implicit val instancePersistenceReads: Reads[Instance] = {
    import Template.templatePersistenceReads
    Json.reads[Instance]
  }

  /**
    * Remove secrets from an instance.
    *
    * This instance removes all values of parameters marked as secrets from the instance parameters.
    */
  implicit val instanceRemoveSecrets: RemoveSecrets[Instance] = RemoveSecrets.instance { instance =>
    // FIXME "censoring" through setting the values null is ugly but using Option[String] gives me stupid Json errors
    val parameterInfos = instance.template.parameterInfos
    instance.copy(parameterValues = instance.parameterValues.map {
      case (parameter, value) =>
        val possiblyCensoredValue = if (parameterInfos.get(parameter).exists(_.secret.contains(true))) {
          null
        } else {
          value
        }
        (parameter, possiblyCensoredValue)
    })
  }

}
