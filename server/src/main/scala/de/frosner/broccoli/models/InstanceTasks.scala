package de.frosner.broccoli.models

import play.api.libs.json.{Json, Writes}

/**
  * The tasks of an instance.
  *
  * @param instanceId The ID of the instance
  * @param tasks The tasks of the instance
  */
final case class InstanceTasks(instanceId: String, tasks: Seq[Task])

object InstanceTasks {
  implicit val instanceTasksWrites: Writes[InstanceTasks] = Json.writes[InstanceTasks]
}
