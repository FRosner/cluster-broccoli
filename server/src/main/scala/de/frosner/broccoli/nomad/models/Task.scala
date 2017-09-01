package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import shapeless.tag
import shapeless.tag.@@

final case class Task(name: String @@ Task.Name, resources: Resources, services: Option[Seq[Service]])

object Task {
  sealed trait Name

  implicit val taskFormat: Format[Task] = (
    (JsPath \ "Name").format[String].inmap[String @@ Name](tag[Name](_), identity)
      and (JsPath \ "Resources").format[Resources] and
      (JsPath \ "Services").formatNullable[Seq[Service]]
  )(Task.apply, unlift(Task.unapply))
}
