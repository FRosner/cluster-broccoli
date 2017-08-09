package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHTTPResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results
import play.mvc.Http.HeaderNames

/**
  * An instance was created.
  *
  * @param instanceCreation Instance creation parameters
  * @param instanceWithStatus The new instance and its status
  */
final case class InstanceCreated(instanceCreation: InstanceCreation, instanceWithStatus: InstanceWithStatus)

object InstanceCreated {
  implicit val instanceCreatedWrites: Writes[InstanceCreated] = Json.writes[InstanceCreated]

  /**
    * Convert an instance deleted result to an HTTP result.
    *
    * The HTTP result is 201 Created with the new resource value, ie, the status of the new instance, in the JSON body,
    * and a Location header with the HTTP resource URL of the new instance.
    */
  implicit val instanceCreatedToHTTPResult: ToHTTPResult[InstanceCreated] = ToHTTPResult.instance { value =>
    Results
      .Created(Json.toJson(value.instanceWithStatus))
      .withHeaders(HeaderNames.LOCATION -> s"/api/v1/instances/${value.instanceWithStatus.instance.id}")
  }
}
