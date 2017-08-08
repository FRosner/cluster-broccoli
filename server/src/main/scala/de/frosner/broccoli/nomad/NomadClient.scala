package de.frosner.broccoli.nomad

import de.frosner.broccoli.nomad.models.{Allocation, WithId}

import scala.collection.immutable
import scala.concurrent.Future

/**
  * A client for Nomad.
  */
trait NomadClient {

  /**
    * Get allocations for a job.
    *
    * @param jobId The ID of the job
    * @return The list of allocations for the job
    */
  def getAllocationsForJob(jobId: String): Future[WithId[immutable.Seq[Allocation]]]
}
