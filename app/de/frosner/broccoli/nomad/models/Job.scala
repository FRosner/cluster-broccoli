package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Job(taskGroups: Option[Seq[TaskGroup]])

object Job {

  implicit val jobFormat: Format[Job] =
    (__ \ "TaskGroups")
      .formatNullable[Seq[TaskGroup]]
      .inmap(taskGroups => Job(taskGroups), (job: Job) => job.taskGroups)

}
