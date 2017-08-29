package de.frosner.broccoli.models

import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import play.api.libs.json.{Json, Writes}

/**
  * An allocated task.
  *
  * @param taskName The name of the task
  * @param taskState The state of the taskj
  * @param allocationId The ID of allocation running the task
  * @param clientStatus The client status of the allocation
  */
final case class AllocatedTask(taskName: String, taskState: TaskState, allocationId: String, clientStatus: ClientStatus)

object AllocatedTask {
  implicit val allocatedTaskWrites: Writes[AllocatedTask] = Json.writes[AllocatedTask]
}
