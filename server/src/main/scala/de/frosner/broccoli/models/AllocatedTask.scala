package de.frosner.broccoli.models

import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import play.api.libs.json.{Json, Writes}
import squants.Quantity
import squants.information.{Bytes, Information}
import squants.time.Frequency

/**
  * An allocated task.
  *
  * @param taskName The name of the task
  * @param taskState The state of the taskj
  * @param allocationId The ID of allocation running the task
  * @param clientStatus The client status of the allocation
  * @param cpuTicksUsed The CPU ticks of the task if known
  * @param memoryUsed The memory use of the task if known
  */
final case class AllocatedTask(
    taskName: String,
    taskState: TaskState,
    allocationId: String,
    clientStatus: ClientStatus,
    cpuTicksUsed: Option[Frequency],
    memoryUsed: Option[Information]
)

object AllocatedTask {
  implicit val allocatedTaskWrites: Writes[AllocatedTask] = Writes { task =>
    Json.obj(
      "taskName" -> task.taskName,
      "taskState" -> task.taskState,
      "allocationId" -> task.allocationId,
      "clientStatus" -> task.clientStatus,
      "cpuTicksMhzUsed" -> task.cpuTicksUsed.map(_.toMegahertz),
      "memoryBytesUsed" -> task.memoryUsed.map(_.toBytes.toInt)
    )
  }
}
