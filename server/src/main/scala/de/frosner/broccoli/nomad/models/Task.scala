package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Task(services: Option[Seq[Service]], resources: Resources)

object Task {
  sealed trait Name

  implicit val taskFormat: Format[Task] = (
    (JsPath \ "Services").formatNullable[Seq[Service]] and (JsPath \ "Resources").format[Resources]
  )(Task.apply, unlift(Task.unapply))
}
