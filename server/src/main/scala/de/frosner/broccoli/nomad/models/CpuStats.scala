package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@

/**
  * Statistics about CPU usage
  *
  * @param totalTicks The CPU ticks consumed
  */
final case class CpuStats(totalTicks: Double @@ CpuStats.TotalTicks)

object CpuStats {
  sealed trait TotalTicks

  implicit val cpuStatsReads: Reads[CpuStats] =
    (JsPath \ "TotalTicks").read[Double].map(tag[CpuStats.TotalTicks](_)).map(CpuStats(_))
}
