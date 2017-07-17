package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class TaskGroup(tasks: Option[Seq[Task]])

object TaskGroup {

  implicit val taskGroupReads: Reads[TaskGroup] =
    (JsPath \ "Tasks").readNullable[Seq[Task]].map(TaskGroup.apply)

}
