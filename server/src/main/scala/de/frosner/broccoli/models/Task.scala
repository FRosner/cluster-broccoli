package de.frosner.broccoli.models

import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import play.api.libs.json.{Json, Writes}

/**
  * A task and the allocations it runs on.
  *
  * @param name The name of the task
  * @param allocations The allocations this task runs on
  */
final case class Task(name: String, allocations: Seq[Task.Allocation])

object Task {
  implicit val taskWrites: Writes[Task] = Json.writes[Task]

  /**
    * A single allocation for a Task
    *
    * @param id The ID of the allocation
    * @param clientStatus The client (=instance) status of this allocation
    * @param taskState The state of the task in this allocation
    */
  final case class Allocation(id: String, clientStatus: ClientStatus, taskState: TaskState)

  object Allocation {
    implicit val allocationWrites: Writes[Allocation] = Json.writes[Allocation]
  }
}
