package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.immutable

// TODO try making this a trait, like a decorator
case class InstanceWithStatus(instance: Instance,
                              status: JobStatus,
                              services: Seq[Service],
                              periodicRuns: Seq[PeriodicRun])
    extends Serializable

object InstanceWithStatus {
  import Instance.instanceApiWrites

  implicit val instanceWithStatusWrites: Writes[InstanceWithStatus] =
    (JsPath.write[Instance] and
      (JsPath \ "status").write[JobStatus] and
      (JsPath \ "services").write[Seq[Service]] and
      (JsPath \ "periodicRuns")
        .write[Seq[PeriodicRun]])(unlift(InstanceWithStatus.unapply))

}
