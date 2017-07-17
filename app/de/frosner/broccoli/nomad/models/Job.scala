package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Job(taskGroups: Option[Seq[TaskGroup]])

object Job {

  implicit val jobReads: Reads[Job] = (JsPath \ "TaskGroups").readNullable[Seq[TaskGroup]].map(Job.apply)

}
