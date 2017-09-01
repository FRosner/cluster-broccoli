package de.frosner.broccoli.models

import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import play.api.libs.json.{Json, Writes}
import shapeless.tag.@@
import squants.information.Information
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
    resources: AllocatedTask.Resources
)

object AllocatedTask {
  sealed trait CPURequired
  sealed trait CPUUsed
  sealed trait MemoryRequired
  sealed trait MemoryUsed

  final case class Resources(
      cpuRequired: Option[Frequency] @@ CPURequired,
      cpuUsed: Option[Frequency] @@ CPUUsed,
      memoryRequired: Option[Information] @@ MemoryRequired,
      memoryUsed: Option[Information] @@ MemoryUsed
  )

  object Resources {
    implicit val resourcesTaskWrites: Writes[Resources] = Writes { resources =>
      Json
        .obj(
          "cpuRequiredMhz" -> resources.cpuRequired.map(_.toMegahertz),
          "cpuUsedMhz" -> resources.cpuUsed.map(_.toMegahertz),
          "memoryRequiredBytes" -> resources.memoryRequired.map(_.toBytes),
          "memoryUsedBytes" -> resources.memoryUsed.map(_.toBytes)
        )
    }
  }

  implicit val allocatedTaskWrites: Writes[AllocatedTask] = Writes { task =>
    Json.obj(
      "taskName" -> task.taskName,
      "taskState" -> task.taskState,
      "allocationId" -> task.allocationId,
      "clientStatus" -> task.clientStatus,
      "resources" -> task.resources
    )
  }
}
