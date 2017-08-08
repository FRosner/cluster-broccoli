package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHTTPResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results

/**
  * An instance error.
  */
sealed trait InstanceError {

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

  /**
    * The ID of an instance was missing.
    */
  final case object IdMissing extends InstanceError {
    override val reason: String = "Instance ID missing"
  }

  /**
    * A user was denied to perform an operation on an instance because their instance regex didn't allow access to the
    * instance.
    *
    * @param instanceId The ID of the instance
    * @param regex The instance regex that was used in attempted access to the instance
    */
  final case class UserRegexDenied(instanceId: String, regex: String) extends InstanceError {
    override val reason: String = s"Operation on $instanceId denied, not matching $regex"
  }

  /**
    * A user was denied to perform an operation on an instance because the operation required administrative privileges
    * which the user lacked.
    */
  final case object AdministratorRequired extends InstanceError {
    override val reason: String = s"Administrator role required"
  }

  /**
    * A generic error thrown while operating on an instance.
    *
    * @param throwable The throwable thrown by the operation
    */
  @deprecated(message = "Do not use, map throwables to specific instance errors or propagate them to trigger ISEs",
              since = "2017-08-08")
  final case class Generic(throwable: Throwable) extends InstanceError {
    override val reason: String = throwable.toString
  }

  implicit val instanceErrorWrites: Writes[InstanceError] = Writes { value =>
    Json.obj("reason" -> value.reason)
  }

  implicit val instanceErrorToHTTPResult: ToHTTPResult[InstanceError] = ToHTTPResult.instance {
    case value: NotFound        => Results.NotFound(value.reason)
    case IdMissing              => Results.BadRequest(IdMissing.reason)
    case value: UserRegexDenied => Results.Forbidden(value.reason)
    case AdministratorRequired  => Results.Forbidden(AdministratorRequired.reason)
    case value: Generic         => Results.BadRequest(value.reason)
  }
}
