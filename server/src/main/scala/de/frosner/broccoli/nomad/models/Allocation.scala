package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

/**
  * A partial model for a single allocations.
  */
final case class Allocation(id: String, nodeId: String, taskStates: Map[String, TaskStateEvents])

object Allocation {
  implicit val allocationReads: Reads[Allocation] =
    ((JsPath \ "ID").read[String] and
      (JsPath \ "NodeID").read[String] and
      (JsPath \ "TaskStates").readNullable[Map[String, TaskStateEvents]].map(_.getOrElse(Map.empty)))(
      Allocation.apply _)
}
