package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHTTPResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results

/**
  * A deleted instance.
  *
  * @param instanceId The instance ID
  * @param instance The last state of the instance
  */
final case class InstanceDeleted(instanceId: String, instance: InstanceWithStatus)

object InstanceDeleted {
  implicit val instanceDeletedWrites: Writes[InstanceDeleted] = Json.writes[InstanceDeleted]

  /**
    * Convert an instance deleted result to an HTTP result.
    *
    * The HTTP result is 200 OK with last resource value, ie, the last known instance status, in the JSON body.
    */
  implicit val instanceDeletedToHTTPResult: ToHTTPResult[InstanceDeleted] = ToHTTPResult.instance { value =>
    Results.Ok(Json.toJson(value.instance))
  }
}
