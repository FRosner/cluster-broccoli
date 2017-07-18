package de.frosner.broccoli.models

import play.api.libs.json.Json

import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import InstanceWithStatus.{instanceWithStatusWrites}

sealed trait InstanceCreationResult {
  val instanceCreation: InstanceCreation
}

case class InstanceCreationSuccess(instanceCreation: InstanceCreation, instanceWithStatus: InstanceWithStatus)
    extends InstanceCreationResult

case class InstanceCreationFailure(instanceCreation: InstanceCreation, reason: String) extends InstanceCreationResult

object InstanceCreationSuccess {

  implicit val instanceCreationSuccessWrites = Json.writes[InstanceCreationSuccess]

}

object InstanceCreationFailure {

  implicit val instanceCreationFailureWrites = Json.writes[InstanceCreationFailure]

}
