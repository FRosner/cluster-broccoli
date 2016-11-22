package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json._

object JobStatus extends Enumeration {

  type JobStatus = Value

  val Running = Value("running")
  val Pending = Value("pending")
  val Stopped = Value("stopped")
  val Dead = Value("dead")
  val Unknown = Value("unknown")

}

object JobStatusJson {

  implicit val instanceStatusWrites: Writes[JobStatus] = Writes(value => JsString(value.toString))

  implicit val instanceStatusReads: Reads[JobStatus] = Reads(_.validate[String].map(JobStatus.withName))

}
