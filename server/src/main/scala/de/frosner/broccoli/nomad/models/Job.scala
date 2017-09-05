package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Job(taskGroups: Seq[TaskGroup])

object Job {
  sealed trait Id

  implicit val jobFormat: Format[Job] =
    (JsPath \ "TaskGroups")
      .format[Seq[TaskGroup]]
      .inmap(taskGroups => Job(taskGroups), (job: Job) => job.taskGroups)

}
