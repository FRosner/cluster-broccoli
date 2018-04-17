package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import JobStatusJson.{jobStatusReads, jobStatusWrites}
import play.api.libs.json.{JsValue, Json}

case class InstanceUpdate(
    instanceId: Option[String], // Option because we don't need it from the HTTP API, only for the websocket
    status: Option[JobStatus],
    parameterValues: Option[Map[String, JsValue]],
    periodicJobsToStop: Option[List[String]],
    selectedTemplate: Option[String])

object InstanceUpdate {

  implicit val instanceUpdateWrites = Json.writes[InstanceUpdate]

  implicit val instanceUpdateReads = Json.reads[InstanceUpdate]

}
