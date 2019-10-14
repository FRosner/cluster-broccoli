package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.nomad.NomadConfiguration
import play.api.libs.json._
import play.api.libs.json.JsNull

import scala.util.{Failure, Success, Try}

case class Instance(id: String, template: Template, parameterValues: Map[String, ParameterValue]) extends Serializable {

  def requireParameterValueConsistency(parameterValues: Map[String, ParameterValue], template: Template) = {
    val realParametersWithValues = parameterValues.keySet ++ template.parameterInfos.flatMap {
      case (key, info) => info.default.map(Function.const(key))
    }
    require(
      template.parameters == realParametersWithValues,
      s"The given parameters values ($realParametersWithValues) " +
        s"need to match the ones in the template (${template.parameters}) (instance id $id)."
    )
  }

  def namespace(implicit nomadConfiguration: NomadConfiguration): Option[String] =
    parameterValues.get(nomadConfiguration.namespaceVariable).flatMap {
      case StringParameterValue(value) => Some(value)
      case _                           => None
    }

  requireParameterValueConsistency(parameterValues, template)
}

object Instance {

  implicit def instanceApiWrites(implicit account: Account): Writes[Instance] = {
    import Template.templateApiWrites
    new Writes[Instance] {
      override def writes(instance: Instance) = {
        val jsValueMap =
          instance.parameterValues.map {
            case (paramName, paramValue) =>
              // TODO: Shouldn't use nulls, Fix this after fixing instanceRemoveSecrets
              (paramName, Option(paramValue).map(_.asJsValue).getOrElse(JsNull))
          }
        Json.obj(
          "id" -> instance.id,
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
              // TODO: Shouldn't use nulls, Fix this after fixing instanceRemoveSecrets
              (paramName, Option(paramValue).map(_.asJsValue).getOrElse(JsNull))
          }
        Json.obj(
          "id" -> instance.id,
          "template" -> instance.template,
          "parameterValues" -> jsValueMap
        )
      }
    }
  }

  implicit val instancePersistenceReads: Reads[Instance] = {
    import Template.templatePersistenceReads
    import Template.jsonFormat
    new Reads[Instance] {
      override def reads(json: JsValue): JsResult[Instance] =
        Try {
          val id: String = (json \ "id").as[String]
          val template: Template = (json \ "template").as[Template]
          val parameterValues: Map[String, ParameterValue] =
            (json \ "parameterValues")
              .as[JsObject]
              .value
              .map {
                case (paramName, paramJsValue) =>
                  paramJsValue match {
                    // TODO: Shouldn't use nulls, Fix this after fixing instanceRemoveSecrets
                    case JsNull =>
                      (paramName, null)
                    case _ =>
                      ParameterValue.fromJsValue(paramName, template.parameterInfos, paramJsValue) match {
                        case Success(paramValue) => (paramName, paramValue)
                        case Failure(ex)         => throw ex
                      }
                  }
              }
              .toMap // We need to cast an unmodifiable Map to an immutable one. What is the cost for this?
          Instance(id, template, parameterValues)
        } match {
          case Success(s)  => JsSuccess(s)
          case Failure(ex) => JsError(ex.getMessage)
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
