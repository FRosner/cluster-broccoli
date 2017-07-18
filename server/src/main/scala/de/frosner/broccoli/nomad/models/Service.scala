package de.frosner.broccoli.nomad.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Service(name: String)

object Service {

  implicit val serviceFormat: Format[Service] =
    (__ \ "Name")
      .format[String]
      .inmap(name => Service(name), (service: Service) => service.name)

}
