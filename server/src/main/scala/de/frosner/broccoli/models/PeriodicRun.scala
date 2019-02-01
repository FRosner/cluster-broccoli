package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json.Json
import JobStatusJson._

case class PeriodicRun(createdBy: String, status: JobStatus, utcSeconds: Long, jobName: String) extends Serializable

object PeriodicRun {

  implicit val periodicRunWrites = Json.writes[PeriodicRun]

  implicit val periodicRunReads = Json.reads[PeriodicRun]

}