package de.frosner.broccoli.models

import play.api.libs.json._

object InstanceStatus extends Enumeration {

  type InstanceStatus = Value

  val Running = Value("running")
  val Pending = Value("pending")
  val Stopped = Value("stopped")
  val Unknown = Value("unknown")

  implicit val instanceStatusWrites: Writes[InstanceStatus] = Writes(value => JsString(value.toString))

  implicit val instanceStatusReads: Reads[InstanceStatus] = Reads(_.validate[String].map(withName))
}
