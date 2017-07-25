package de.frosner.broccoli.models

import play.api.libs.json.{Json, Reads}

/**
  * Represents meta information of the template
  * @param description Brief description of the template
  * @param parameters Additional properties for the template parameters (e.g. default value, if the parameter is a secret, etc)
  */
final case class Meta(description: Option[String], parameters: Option[Map[String, Meta.Parameter]])

object Meta {

  /**
    *
    * @param name Name of the parameter
    * @param default Default value of the parameter
    * @param secret True if the parameter is a secret
    */
  final case class Parameter(name: Option[String], default: Option[String], secret: Option[Boolean])

  object Parameter {
    implicit val parameterReads: Reads[Parameter] = Json.reads[Parameter]
  }

  implicit val metaReads: Reads[Meta] = Json.reads[Meta]
}
