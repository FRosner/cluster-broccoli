package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OFormat}
import shapeless.tag
import shapeless.tag.@@
import squants.information.{Information, Megabytes}
import squants.time.{Frequency, Megahertz}

/**
  * Resources a task requires.
  *
  * @param cpu The CPU share required for the task
  * @param memory The memory required for the task
  */
final case class Resources(cpu: Frequency @@ Resources.CPU, memory: Information @@ Resources.Memory)

object Resources {
  sealed trait CPU
  sealed trait Memory

  implicit val resourcesFormat: OFormat[Resources] = (
    (JsPath \ "CPU")
      .format[Double]
      .inmap[Frequency @@ CPU](mhz => tag[CPU](Megahertz(mhz)), _.toMegahertz)
      and
        (JsPath \ "MemoryMB")
          .format[Double]
          .inmap[Information @@ Memory](mb => tag[Memory](Megabytes(mb)), _.toMegabytes)
  )(Resources.apply, unlift(Resources.unapply))
}
