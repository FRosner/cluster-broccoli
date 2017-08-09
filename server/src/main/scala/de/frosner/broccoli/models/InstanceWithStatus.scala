package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.immutable

// TODO try making this a trait, like a decorator
case class InstanceWithStatus(instance: Instance,
                              status: JobStatus,
                              services: Seq[Service],
                              periodicRuns: Seq[PeriodicRun])
    extends Serializable

object InstanceWithStatus {
  import Instance.instanceApiWrites

  implicit val instanceWithStatusWrites: Writes[InstanceWithStatus] =
    (JsPath.write[Instance] and
      (JsPath \ "status").write[JobStatus] and
      (JsPath \ "services").write[Seq[Service]] and
      (JsPath \ "periodicRuns")
        .write[Seq[PeriodicRun]])(unlift(InstanceWithStatus.unapply))

  /**
    * Remove secrets from the parameters of an instance if the given role may not see them.
    *
    * Only Administrators are permitted to see secrets.
    *
    * @param role The role to use to access the instance secrets
    * @param instance The instance
    * @return The original instance if the role is Administrator, otherwise the role without secret parameters
    */
  def filterSecretsForRole(role: Role)(instance: InstanceWithStatus): InstanceWithStatus = role match {
    case Role.Administrator => instance
    case _                  => removeSecretVariables(instance)
  }

  /**
    * Remove secrets from the parameters of an instance.
    *
    * @param instanceWithStatus The instance to remove secrets from
    * @return The instance with secrets removed
    */
  def removeSecretVariables(instanceWithStatus: InstanceWithStatus): InstanceWithStatus = {
    // FIXME "censoring" through setting the values null is ugly but using Option[String] gives me stupid Json errors
    val instance = instanceWithStatus.instance
    val template = instance.template
    val parameterInfos = template.parameterInfos
    val newParameterValues = instance.parameterValues.map {
      case (parameter, value) =>
        val possiblyCensoredValue = if (parameterInfos.get(parameter).exists(_.secret == Some(true))) {
          null.asInstanceOf[String]
        } else {
          value
        }
        (parameter, possiblyCensoredValue)
    }
    instanceWithStatus.copy(
      instance = instanceWithStatus.instance.copy(
        parameterValues = newParameterValues
      )
    )
  }
}
