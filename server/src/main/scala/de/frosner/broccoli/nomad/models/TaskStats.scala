package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}

/**
  * Resource usage of an individual task.
  *
  * @param resourceUsage The resource usage
  */
final case class TaskStats(resourceUsage: ResourceUsage)

object TaskStats {
  implicit val taskStatsReads: Reads[TaskStats] = (JsPath \ "ResourceUsage").read[ResourceUsage].map(TaskStats(_))
}
