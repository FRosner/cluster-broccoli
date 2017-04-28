package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.{JobStatus}
import JobStatusJson.{jobStatusReads, jobStatusWrites}
import play.api.libs.json.Json

case class InstanceUpdate(status: Option[JobStatus], parameterValues: Option[Map[String, String]], selectedTemplate: Option[String])

object InstanceUpdate {

  implicit val instanceUpdateWrites = Json.writes[InstanceUpdate]

  implicit val instanceUpdateReads = Json.reads[InstanceUpdate]

}

