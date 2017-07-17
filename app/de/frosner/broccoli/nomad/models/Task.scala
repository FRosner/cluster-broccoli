package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Task(services: Option[Seq[Service]])

object Task {

  implicit val taskReads: Reads[Task] =
    (JsPath \ "Services").readNullable[Seq[Service]].map(Task.apply)

}
