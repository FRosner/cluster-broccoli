package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

// TODO try making this a trait, like a decorator
case class InstanceWithStatus(instance: Instance,
                              status: JobStatus,
                              services: Map[String, Service],
                              periodicRuns: Iterable[PeriodicRun]) extends Serializable

object InstanceWithStatus {

  implicit val instanceWithStatusWrites: Writes[InstanceWithStatus] = {
    import Instance.instanceApiWrites
    val writePath = JsPath.write[Instance] and
      (JsPath \ "status").write[JobStatus] and
      (JsPath \ "services").write[Map[String, Service]] and
      (JsPath \ "periodicRuns").write[Iterable[PeriodicRun]]
    writePath((instanceWithStatus: InstanceWithStatus) =>
      (instanceWithStatus.instance,
        instanceWithStatus.status,
        instanceWithStatus.services,
        instanceWithStatus.periodicRuns)
    )
  }

}
