package de.frosner.broccoli.models

import play.api.libs.json.Json

case class Service(name: String, protocol: String, address: String, port: Int) extends Serializable

object Service {

  implicit val instanceWrites = Json.writes[Service]

  implicit val instanceReads = Json.reads[Service]

}