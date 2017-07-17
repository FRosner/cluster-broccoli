package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

final case class TaskStateEvents(state: TaskState)

object TaskStateEvents {

  implicit val taskStateEventsReads: Reads[TaskStateEvents] =
    (JsPath \ "State").read[TaskState].map(TaskStateEvents(_))
}
