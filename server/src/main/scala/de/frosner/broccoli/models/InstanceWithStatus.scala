package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets
import de.frosner.broccoli.RemoveSecrets.ops._
import de.frosner.broccoli.models.JobStatus.JobStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

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

  /**
    * Remove secrets from this instance.
    *
    * This instance removes secrets from the contained Instance.
    *
    * @param removeInstanceSecrets RemoveSecrets instance for Instance type.
    * @return An instance for InstanceWithStatus
    */
  implicit def instanceWithStatusRemoveSecrets(
      implicit removeInstanceSecrets: RemoveSecrets[Instance]
  ): RemoveSecrets[InstanceWithStatus] =
    RemoveSecrets.instance(instance => instance.copy(instance = instance.instance.removeSecrets))
}
