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
    * @param throwable An optional exception denoting the missing instance
    */
  final case class NotFound(instanceId: String, throwable: Option[Throwable] = None) extends InstanceError {
    override val reason: String =
      throwable.map(t => s"Instance $instanceId not found: $t").getOrElse(s"Instance $instanceId not found")
  }

  /**
    * The ID of an instance was missing.
    */
  final case object IdMissing extends InstanceError {
    override val reason: String = "Instance ID missing"
  }

  /**
    * The template of an instance was not found.
    *
    * @param templateId The template ID
    */
  final case class TemplateNotFound(templateId: String) extends InstanceError {
    override val reason: String = s"Template $templateId not found"
  }

  /**
    * Instance parameters were invalid.
    *
    * @param reason A description of why parameters were invalid
    */
  final case class InvalidParameters(reason: String) extends InstanceError

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
    * A user was denied to perform an operation on an instance because the operation requires roles
    * which the user lacked.
    */
  final case class RolesRequired(roles: Set[Role]) extends InstanceError {
    override val reason: String = s"Requires the following role(s): ${roles.toSeq.sortBy(_.toString).mkString(", ")}"
  }

  object RolesRequired {
    def apply(role: Role, roles: Role*): RolesRequired = RolesRequired(Set(role) ++ roles)
  }

  /**
    * Nomad is not reachable.
    */
  final case object NomadUnreachable extends InstanceError {
    override val reason: String = "Nomad unreachable"
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

  /**
    * Convert an instance error to the corresponding HTTP status
    *
    * @param error The error
    * @return The corresponding HTTP status.
    */
  def toStatus(error: InstanceError): Results.Status = error match {
    case _: NotFound          => Results.NotFound
    case IdMissing            => Results.BadRequest
    case _: TemplateNotFound  => Results.BadRequest
    case _: InvalidParameters => Results.BadRequest
    case _: UserRegexDenied   => Results.Forbidden
    case _: RolesRequired     => Results.Forbidden
    case NomadUnreachable     => Results.NotFound
    case _: Generic           => Results.BadRequest
  }

  implicit val instanceErrorToHTTPResult: ToHTTPResult[InstanceError] =
    ToHTTPResult.instance(error => toStatus(error)(Json.toJson(error)))
}
