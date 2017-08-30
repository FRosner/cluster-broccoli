package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@
import squants.time.{Frequency, Megahertz}

/**
  * Statistics about CPU usage
  *
  * @param totalTicks The CPU ticks consumed
  */
final case class CpuStats(totalTicks: Frequency @@ CpuStats.TotalTicks)

object CpuStats {
  sealed trait TotalTicks

  implicit val cpuStatsReads: Reads[CpuStats] = for {
    ticks <- (JsPath \ "TotalTicks").read[Double].map(ticks => tag[CpuStats.TotalTicks](Megahertz(ticks)))
  } yield CpuStats(ticks)
}
