package de.frosner.broccoli.models

import de.frosner.broccoli.models.InstanceStatus.InstanceStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

// TODO try making this a trait, like a decorator
case class InstanceWithStatus(instance: Instance, status: InstanceStatus, services: Map[String, Service]) extends Serializable

object InstanceWithStatus {

  implicit val instanceWithStatusWrites: Writes[InstanceWithStatus] = {
    import Instance.instanceApiWrites
    (
      (JsPath).write[Instance] and
      (JsPath \ "status").write[InstanceStatus] and
      (JsPath \ "services").write[Map[String, Service]]
      )((instanceWithStatus: InstanceWithStatus) => (instanceWithStatus.instance, instanceWithStatus.status, instanceWithStatus.services)
    )
  }

}
