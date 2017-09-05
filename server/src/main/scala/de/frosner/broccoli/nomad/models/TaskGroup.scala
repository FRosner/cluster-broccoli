package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class TaskGroup(tasks: Seq[Task])

object TaskGroup {

  implicit val taskGroupFormat: Format[TaskGroup] =
    (__ \ "Tasks")
      .format[Seq[Task]]
      .inmap(tasks => TaskGroup(tasks), (taskGroup: TaskGroup) => taskGroup.tasks)

}
