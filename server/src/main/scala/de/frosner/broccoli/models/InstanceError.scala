package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHttpResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results

/**
  * An instance error.
  */
sealed trait InstanceError {

  /**
    * The ID of the instance that triggered the error,
    */
  def instanceId: String

  /**
    * A human-readable reason for this error.
    */
  def reason: String
}

object InstanceError {

  /**
    * An instance wasn't found.
    *
    * @param instanceId The instance ID
    */
  final case class NotFound(instanceId: String) extends InstanceError {
    override val reason: String = s"Instance $instanceId not found"
  }

  implicit val instanceErrorWrites: Writes[InstanceError] = Writes { value =>
    Json.obj("instanceId" -> value.instanceId, "reason" -> value.reason)
  }

  implicit val instanceErrorToHttpResult: ToHttpResult[InstanceError] = ToHttpResult.instance {
    case value: NotFound => Results.NotFound(value.reason)
  }
}
