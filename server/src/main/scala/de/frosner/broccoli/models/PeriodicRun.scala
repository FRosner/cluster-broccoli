package de.frosner.broccoli.models

import java.util.Date

import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import play.api.libs.json.Json
import JobStatusJson._

case class PeriodicRun(createdBy: String, status: JobStatus, utcSeconds: Long, jobName: String) extends Serializable

object PeriodicRun {

  implicit val periodicRunWrites = Json.writes[PeriodicRun]

  implicit val periodicRunReads = Json.reads[PeriodicRun]

}