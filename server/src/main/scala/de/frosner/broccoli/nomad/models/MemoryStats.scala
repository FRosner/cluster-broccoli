package de.frosner.broccoli.nomad.models

import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@
import squants.information.{Bytes, Information}

/**
  * Memory statistics.
  *
  * @param rss Residual memory in bytes
  */
final case class MemoryStats(rss: Information @@ MemoryStats.RSS)

object MemoryStats {
  sealed trait RSS

  implicit val memoryStatsReads: Reads[MemoryStats] = (JsPath \ "RSS")
    .read[Long]
    .map(bytes => tag[RSS](Bytes(bytes)))
    .map(MemoryStats(_))

}
