package de.frosner.broccoli.models

import play.api.libs.json._

object InstanceStatus extends Enumeration {

  type InstanceStatus = Value

  val Starting = Value("starting")
  val Started = Value("started")
  val Stopped = Value("stopped")
  val Stopping = Value("stopping")
  val Unknown = Value("unknown")

  implicit val instanceStatusWrites: Writes[InstanceStatus] = Writes(value => JsString(value.toString))

  implicit val instanceStatusReads: Reads[InstanceStatus] = Reads(_.validate[String].map(withName))
}
