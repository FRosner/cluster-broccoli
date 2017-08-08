package de.frosner.broccoli.models

import play.api.libs.json.{Json, Writes}

/**
  * A deleted instance.
  *
  * @param instanceId The instance ID
  * @param instance The last state of the instance
  */
final case class InstanceDeleted(instanceId: String, instance: InstanceWithStatus)

object InstanceDeleted {
  implicit val instanceDeletedWrites: Writes[InstanceDeleted] = Json.writes[InstanceDeleted]
}
