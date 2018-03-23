package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class Instance(id: String, template: Template, parameterValues: Map[String, ParameterValue]) extends Serializable {

  def requireParameterValueConsistency(parameterValues: Map[String, ParameterValue], template: Template) = {
    val realParametersWithValues = parameterValues.keySet ++ template.parameterInfos.flatMap {
      case (key, info) => info.default.map(Function.const(key))
    }
    require(
      template.parameters == realParametersWithValues,
      s"The given parameters values (${parameterValues.keySet}) " +
        s"need to match the ones in the template (${template.parameters})."
    )
  }

  requireParameterValueConsistency(parameterValues, template)

  def updateParameterValues(newParameterValues: Map[String, ParameterValue]): Try[Instance] =
    Try {
      requireParameterValueConsistency(newParameterValues, template)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = this.template,
        parameterValues = newParameterValues
      )
    }

  def updateTemplate(newTemplate: Template, newParameterValues: Map[String, ParameterValue]): Try[Instance] =
    Try {
      requireParameterValueConsistency(newParameterValues, newTemplate)
      require(newParameterValues("id") == parameterValues("id"), s"The parameter value 'id' must not be changed.")

      Instance(
        id = this.id,
        template = newTemplate,
        parameterValues = newParameterValues
      )
    }

}

object Instance {

  implicit val instanceApiWrites: Writes[Instance] = {
    import Template.templateApiWrites
    new Writes[Instance] {
      override def writes(instance: Instance) = {
        val jsValueMap =
          instance.parameterValues.map {
            case (paramName, paramValue) =>
              (paramName, paramValue.asJsValue)
          }
        Json.obj(
          "id"-> instance.id,
          "template" -> instance.template,
          "parameterValues" -> jsValueMap
        )
      }
    }
  }

  implicit val instancePersistenceWrites: Writes[Instance] = {
    import Template.templatePersistenceWrites
    new Writes[Instance] {
      override def writes(instance: Instance) = {
        val jsValueMap =
          instance.parameterValues.map {
            case (paramName, paramValue) =>
              (paramName, paramValue.asJsValue)
          }
        Json.obj(
          "id"-> instance.id,
          "template" -> instance.template,
          "parameterValues" -> jsValueMap
        )
      }
    }
  }

  implicit val instancePersistenceReads: Reads[Instance] = {
    import Template.templatePersistenceReads
    new Reads[Instance] {
      override def reads(json: JsValue): JsResult[Instance] = {
        Try {
          val id: String = (json \ "id").as[String]
          val template: Template = (json \ "template").as[Template]
          val parameterValues: Map[String, ParameterValue] =
            (json \ "parameterValues").as[JsObject].value.map {
              case (paramName, paramJsValue) =>
                ParameterValue.constructParameterValueFromJson(paramName, template, paramJsValue) match {
                  case Success(paramValue) => (paramName, paramValue)
                  case Failure(ex) => throw ex
                }
            }.toMap // We need to cast an unmodifiable Map to an immutable one. What is the cost for this?
          Instance(id, template, parameterValues)
        } match {
          case Success(s) => JsSuccess(s)
          case Failure(ex) => JsError(ex.getMessage)
        }
      }
    }
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
