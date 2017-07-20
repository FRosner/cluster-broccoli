package de.frosner.broccoli.models

import play.api.libs.json.{Json, Reads}

final case class Meta(description: Option[String], parameters: Option[Map[String, Meta.Parameter]])

object Meta {
  final case class Parameter(name: Option[String], default: Option[String], secret: Option[Boolean])

  object Parameter {
    implicit val parameterReads: Reads[Parameter] = Json.reads[Parameter]
  }

  implicit val metaReads: Reads[Meta] = Json.reads[Meta]
}
