package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.http.ToHTTPResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results

/**
  * An instance was updated.
  *
  * @param instanceUpdate The update performed on the instance
  * @param instanceWithStatus The updated instance and its status
  */
final case class InstanceUpdated(instanceUpdate: InstanceUpdate, instanceWithStatus: InstanceWithStatus)

object InstanceUpdated {
  implicit def instanceUpdatedWrites(implicit account: Account): Writes[InstanceUpdated] = Json.writes[InstanceUpdated]

  /**
    * Convert an instance update result to an HTTP result.
    *
    * The HTTP result is 200 OK with the new resource value, ie, the new instance status, in the JSON body.
    */
  implicit def instanceUpdateToHttpResult(implicit account: Account): ToHTTPResult[InstanceUpdated] =
    ToHTTPResult.instance { value =>
      Results.Ok(Json.toJson(value.instanceWithStatus))
    }
}
