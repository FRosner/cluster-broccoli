package de.frosner.broccoli.models

import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import play.api.libs.json._

object ServiceStatus extends Enumeration {

  type ServiceStatus = Value

  val Passing = Value("passing")
  val Failing = Value("failing")
  val Unknown = Value("unknown")

}

object ServiceStatusJson {

  implicit val serviceStatusWrites: Writes[ServiceStatus] = Writes(value => JsString(value.toString))

  implicit val serviceStatusReads: Reads[ServiceStatus] = Reads(_.validate[String].map(ServiceStatus.withName))

}
