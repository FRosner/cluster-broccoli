package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Task(services: Option[Seq[Service]])

object Task {
  sealed trait Name

  implicit val taskFormat: Format[Task] =
    (__ \ "Services")
      .formatNullable[Seq[Service]]
      .inmap(services => Task(services), (task: Task) => task.services)

}
