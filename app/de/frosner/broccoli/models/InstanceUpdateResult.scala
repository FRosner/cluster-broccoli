package de.frosner.broccoli.models

import play.api.libs.json.Json

import InstanceWithStatus.{instanceWithStatusWrites}

sealed trait InstanceUpdateResult {
  val instanceUpdate: InstanceUpdate
}

case class InstanceUpdateSuccess(instanceUpdate: InstanceUpdate,
                                 instanceWithStatus: InstanceWithStatus) extends InstanceUpdateResult

case class InstanceUpdateFailure(instanceUpdate: InstanceUpdate,
                                 reason: String) extends InstanceUpdateResult

object InstanceUpdateSuccess {

  implicit val instanceUpdateSuccessWrites = Json.writes[InstanceUpdateSuccess]

}

object InstanceUpdateFailure {

  implicit val instanceUpdateFailureWrites = Json.writes[InstanceUpdateFailure]

}
