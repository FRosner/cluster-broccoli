package de.frosner.broccoli.nomad

import cats.instances.future._
import de.frosner.broccoli.nomad.models._
import shapeless.tag.@@
import squants.Quantity
import squants.information.Information

import scala.collection.immutable
import scala.concurrent.ExecutionContext

/**
  * A client for Nomad.
  */
trait NomadClient {

  /**
    * Returns the current version of nomad server which is running
    * @return
    */
  def nomadVersion: String

  /**
    * The execution context for actions of this client.
    */
  implicit def executionContext: ExecutionContext

  /**
    * Get a job.
    *
    * @param jobId The ID of the job
    * @return The job
    */
  def getJob(jobId: String @@ Job.Id, namespace: Option[String]): NomadT[Job]

  /**
    * Get allocations for a job.
    *
    * @param jobId The ID of the job
    * @return The list of allocations for the job
    */
  def getAllocationsForJob(jobId: String @@ Job.Id,
                           namespace: Option[String]): NomadT[WithId[immutable.Seq[Allocation]]]

  /**
    * Get an allocation.
    *
    * @param id The alloction to query
    * @return The allocation or an error
    */
  def getAllocation(id: String @@ Allocation.Id, namespace: Option[String]): NomadT[Allocation]

  /**
    * Get a node.
    *
    * @param id The node to query
    * @return The node data
    */
  def getNode(id: String @@ Node.Id): NomadT[Node]

  /**
    * Get a client to access a specific Nomad node.
    *
    * @param node The node to access
    * @return A client to access the given node.
    */
  def nodeClient(node: Node): NomadNodeClient

  /**
    * Get the client to access the specific Nomad node running the given allocation.
    *
    * @param allocation The allocation whose node to access
    * @return A client for the allocation's node
    */
  def allocationNodeClient(allocation: Allocation): NomadT[NomadNodeClient] =
    for {
      node <- getNode(allocation.nodeId)
    } yield nodeClient(node)
}

/**
  * A client for Nomad nodes.
  *
  * These actions need to be invoked directly on a particular node, ie, require to obtain access to a node first.
  */
trait NomadNodeClient {

  /**
    * Get resource usage statistics of an allocation.
    *
    * @param allocationId The ID of the allocation
    * @return The resource statistics of the allocation with the given ID.
    */
  def getAllocationStats(allocationId: String @@ Allocation.Id, namespace: Option[String]): NomadT[AllocationStats]

  /**
    * Get the log of a task on an allocation.
    *
    * @param allocationId The ID of the allocation
    * @param taskName The name of the task
    * @param stream The kind of log to fetch
    * @param offset The number of bytes to fetch from the end of the log.  If None fetch the entire log
    * @return The task log
    */
  def getTaskLog(
      allocationId: String @@ Allocation.Id,
      taskName: String @@ Task.Name,
      stream: LogStreamKind,
      offset: Option[Quantity[Information] @@ TaskLog.Offset],
      namespace: Option[String]
  ): NomadT[TaskLog]
}
