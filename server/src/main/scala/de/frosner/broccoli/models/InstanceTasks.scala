package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHTTPResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results

/**
  * The tasks of an instance.
  *
  * @param instanceId The ID of the instance
  * @param allocatedTasks Allocated tasks of the instance
  */
final case class InstanceTasks(instanceId: String,
                               allocatedTasks: Seq[AllocatedTask],
                               allocatedPeriodicTasks: Map[String, Seq[AllocatedTask]])

object InstanceTasks {
  implicit val instanceTasksWrites: Writes[InstanceTasks] = Json.writes[InstanceTasks]

  implicit val instanceTasksToHTTPResult: ToHTTPResult[InstanceTasks] =
    ToHTTPResult.instance(v => Results.Ok(Json.toJson(v.allocatedTasks)))
}
