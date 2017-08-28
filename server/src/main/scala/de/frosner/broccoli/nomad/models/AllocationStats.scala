package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import shapeless.tag.@@

/**
  * Allocation statistics.
  *
  * @param resourceUsage Resource usage of the entire allocation
  * @param tasks Resource usage per task
  */
final case class AllocationStats(resourceUsage: ResourceUsage, tasks: Map[String @@ Task.Name, TaskStats])

object AllocationStats {
  implicit val allocationStatsReads: Reads[AllocationStats] =
    ((JsPath \ "ResourceUsage").read[ResourceUsage] and
      (JsPath \ "Tasks").read[Map[String, TaskStats]].map(_.asInstanceOf[Map[String @@ Task.Name, TaskStats]]))(
      AllocationStats.apply _)
}
