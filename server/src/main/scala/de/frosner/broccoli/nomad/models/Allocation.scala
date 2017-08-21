package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@

/**
  * A partial model for a single allocations.
  */
final case class Allocation(
    id: String @@ Allocation.Id,
    jobId: String @@ Job.Id,
    nodeId: String @@ Node.Id,
    clientStatus: ClientStatus,
    taskStates: Map[String @@ Task.Name, TaskStateEvents]
)

object Allocation {
  trait Id

  implicit val allocationReads: Reads[Allocation] =
    ((JsPath \ "ID").read[String].map(tag[Allocation.Id](_)) and
      (JsPath \ "JobID").read[String].map(tag[Job.Id](_)) and
      (JsPath \ "NodeID").read[String].map(tag[Node.Id](_)) and
      (JsPath \ "ClientStatus").read[ClientStatus] and
      (JsPath \ "TaskStates")
        .readNullable[Map[String, TaskStateEvents]]
        // Tag all values as task name. Since Task.Name is a phantom type this is a safe thing to do, albeit it doesn't
        // look like so
        .map(_.getOrElse(Map.empty).asInstanceOf[Map[String @@ Task.Name, TaskStateEvents]]))(Allocation.apply _)
}
