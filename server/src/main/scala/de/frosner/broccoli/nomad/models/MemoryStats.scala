package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@

/**
  * Memory statistics.
  *
  * @param rss Residual memory
  */
final case class MemoryStats(rss: Long @@ MemoryStats.RSS)

object MemoryStats {
  sealed trait RSS

  implicit val memoryStatsReads: Reads[MemoryStats] = (JsPath \ "RSS").read[Long].map(tag[RSS](_)).map(MemoryStats(_))

}
