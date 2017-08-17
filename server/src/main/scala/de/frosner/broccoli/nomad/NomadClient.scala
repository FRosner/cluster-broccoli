package de.frosner.broccoli.nomad

import cats.data.EitherT
import de.frosner.broccoli.nomad.models._
import shapeless.tag.@@

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
  def getAllocationsForJob(jobId: String @@ Job.Id): Future[WithId[immutable.Seq[Allocation]]]

  /**
    * Get an allocation.
    *
    * @param id The alloction to query
    * @return The allocation or an error
    */
  def getAllocation(id: String @@ Allocation.Id): EitherT[Future, NomadError, Allocation]

  /**
    * Get the log of a task on an allocation.
    *
    * @param allocationId The ID of the allocation
    * @param taskName The name of the task
    * @param stream The kind of log to fetch
    * @return The task log
    */
  def getTaskLog(
      allocationId: String @@ Allocation.Id,
      taskName: String @@ Task.Name,
      stream: LogStreamKind
  ): EitherT[Future, NomadError, TaskLog]
}
