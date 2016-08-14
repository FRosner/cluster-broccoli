package de.frosner.broccoli.models

import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import ServiceStatus._
import play.api.libs.json.Json

case class Service(name: String, protocol: String, address: String, port: Int, status: ServiceStatus) extends Serializable

object Service {

  implicit val serviceWrites = Json.writes[Service]

}