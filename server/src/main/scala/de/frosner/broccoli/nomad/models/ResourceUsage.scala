package de.frosner.broccoli.nomad.models

import play.api.libs.json.Reads
import play.api.libs.functional.syntax._

final case class ResourceUsage(cpuStatus: CpuStats, memoryStats: MemoryStats)

object ResourceUsage {
  implicit val resourceUsageReads: Reads[ResourceUsage] = ???
}
