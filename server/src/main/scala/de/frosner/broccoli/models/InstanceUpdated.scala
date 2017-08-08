package de.frosner.broccoli.models

import play.api.libs.json.{Json, Writes}

/**
  * An instance was updated.
  *
  * @param instanceUpdate The update performed on the instance
  * @param instanceWithStatus The updated instance and its status
  */
final case class InstanceUpdated(instanceUpdate: InstanceUpdate, instanceWithStatus: InstanceWithStatus)

object InstanceUpdated {
  implicit val instanceUpdatedWrites: Writes[InstanceUpdated] = Json.writes[InstanceUpdated]
}
