package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

final case class ResourceUsage(cpuStats: CpuStats, memoryStats: MemoryStats)

object ResourceUsage {
  implicit val resourceUsageReads: Reads[ResourceUsage] =
    ((JsPath \ "CpuStats").read[CpuStats] and (JsPath \ "MemoryStats").read[MemoryStats])(ResourceUsage.apply _)
}
