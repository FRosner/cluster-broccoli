package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Job(services: Option[Seq[Service]])

object Job {

  implicit val jobReads: Reads[Job] = (JsPath \ "Services").read[Option[Seq[Service]]].map(Job.apply)

}
