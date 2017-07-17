package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Service(name: String)

object Service {

  implicit val serviceReads: Reads[Service] =
    (JsPath \ "Name").read[String].map(Service.apply)

}
