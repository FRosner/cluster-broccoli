package de.frosner.broccoli.models

import play.api.libs.json.Json

import InstanceWithStatus.{instanceWithStatusWrites}

@deprecated("Replace with Either[InstanceError, InstanceDeletionSuccess]")
sealed trait InstanceDeletionResult {
  val instanceId: String
}

case class InstanceDeletionSuccess(instanceId: String, instanceWithStatus: InstanceWithStatus)
    extends InstanceDeletionResult

@deprecated("Replace with generic InstanceError")
case class InstanceDeletionFailure(instanceId: String, reason: String) extends InstanceDeletionResult

object InstanceDeletionSuccess {

  implicit val instanceDeletionSuccessWrites = Json.writes[InstanceDeletionSuccess]

}

object InstanceDeletionFailure {

  implicit val instanceDeletionFailureWrites = Json.writes[InstanceDeletionFailure]

}
