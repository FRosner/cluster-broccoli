package de.frosner.broccoli.models

import play.api.libs.json.{Json, Writes}

/**
  * An instance was created.
  *
  * @param instanceCreation Instance creation parameters
  * @param instanceWithStatus The new instance and its status
  */
final case class InstanceCreated(instanceCreation: InstanceCreation, instanceWithStatus: InstanceWithStatus)

object InstanceCreated {
  implicit val instanceCreatedWrites: Writes[InstanceCreated] = Json.writes[InstanceCreated]
}
