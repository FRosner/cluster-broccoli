package de.frosner.broccoli.instances

import javax.inject.Inject

import cats.Apply
import cats.data.EitherT
import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.{AllocatedTask, InstanceError, InstanceTasks}
import de.frosner.broccoli.nomad.models.{
  Allocation,
  AllocationStats,
  Job,
  LogStreamKind,
  NomadError,
  TaskLog,
  Task => NomadTask
}
import de.frosner.broccoli.nomad.{NomadClient, NomadT}
import shapeless.tag
import shapeless.tag.@@
import squants.information.Information

import scala.concurrent.{ExecutionContext, Future}

/**
  * Manage broccoli instances on top of Nomad
  *
  * @param nomadClient A client to access the Nomad API.
  */
class NomadInstances @Inject()(nomadClient: NomadClient)(implicit ec: ExecutionContext) {

  type InstanceT[R] = EitherT[Future, InstanceError, R]

  /**
    * Get all allocated tasks of the given instance.
    *
    * In Nomad the hierarchy is normally "allocation -> tasks in that allocation".  However allocations have generic
    * UUID whereas tasks have human-readable names, so we believe that tasks are easier as an "entry point" for the user
    * in the UI.  Hence this method inverts the hierarchy of models returned by Nomad.
    *
    * @param user The user requesting tasks for the instance, for access control
    * @param id The instance ID
    * @return All tasks of the given instance with their allocations, or an empty list if the instance has no tasks or
    *         didn't exist.  If the user may not access the instance return an InstanceError instead.
    */
  def getInstanceTasks(user: Account)(id: String): InstanceT[InstanceTasks] =
    for {
      jobId <- EitherT
        .pure[Future, InstanceError](tag[Job.Id](id))
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex))(_.matches(user.instanceRegex))
      // Request job information and allocations in parallel, to get the required resources for tasks and the actual
      // allocated tasks and their resources.  We can't extract the pair with a pattern unfortunately because as of
      // Scala 2.11 the compiler still tries to insert .withFilter calls even for irrefutable patterns, which EitherT
      // doesn't have.
      jobAndAllocations <- Apply[NomadT]
        .tuple2(nomadClient.getJob(jobId), nomadClient.getAllocationsForJob(jobId))
        .leftMap(toInstanceError(jobId))
      job = jobAndAllocations._1
      allocations = jobAndAllocations._2
      resources <- allocations.payload.toList
        .map { allocation =>
          for {
            node <- nomadClient.allocationNodeClient(allocation)
            stats <- node.getAllocationStats(allocation.id)
          } yield allocation.id -> stats
        }
        .sequence[NomadT, (String @@ Allocation.Id, AllocationStats)]
        .map(_.toMap)
        .leftMap(toInstanceError(jobId))
    } yield {
      val taskResources = (for {
        group <- job.taskGroups
        task <- group.tasks
      } yield task.name -> task.resources).toMap

      InstanceTasks(
        jobId,
        // Flatten tasks into allocated tasks
        allocations.payload
          .flatMap(allocation =>
            allocation.taskStates.map {
              case (taskName, events) =>
                val taskResourceUsage = for {
                  allocationResources <- resources.get(allocation.id)
                  taskResources <- allocationResources.tasks.get(taskName)
                } yield taskResources

                val allocatedTaskResources = AllocatedTask.Resources(
                  tag[AllocatedTask.CPURequired](taskResources.get(taskName).map(_.cpu)),
                  tag[AllocatedTask.CPUUsed](taskResourceUsage.map(_.resourceUsage.cpuStats.totalTicks)),
                  tag[AllocatedTask.MemoryRequired](taskResources.get(taskName).map(_.memory)),
                  tag[AllocatedTask.MemoryUsed](taskResourceUsage.map(_.resourceUsage.memoryStats.rss))
                )

                AllocatedTask(taskName, events.state, allocation.id, allocation.clientStatus, allocatedTaskResources)
          })
          .sortBy(_.taskName)
      )
    }

  def getInstanceLog(user: Account)(
      instanceId: String,
      allocationId: String @@ Allocation.Id,
      taskName: String @@ NomadTask.Name,
      logKind: LogStreamKind,
      offset: Option[Information @@ TaskLog.Offset]
  ): InstanceT[String] =
    for {
      // Check whether the user is allowed to see the instance
      jobId <- EitherT
        .pure[Future, InstanceError](tag[Job.Id](instanceId))
        .ensureOr(InstanceError.UserRegexDenied(_, user.instanceRegex))(_.matches(user.instanceRegex))
      // Check whether the allocation really belongs to the instance.  If it doesn't, ie, if the user tries to access
      // an allocation from another instance hide that the allocation even exists by returning 404
      allocation <- nomadClient
        .getAllocation(allocationId)
        .leftMap(toInstanceError(jobId))
        .ensure(InstanceError.NotFound(instanceId))(_.jobId == jobId)
      node <- nomadClient.allocationNodeClient(allocation).leftMap(toInstanceError(jobId))
      log <- node.getTaskLog(allocationId, taskName, logKind, offset).leftMap(toInstanceError(jobId))
    } yield log.contents

  private def toInstanceError(jobId: String @@ Job.Id)(nomadError: NomadError): InstanceError = nomadError match {
    case NomadError.NotFound    => InstanceError.NotFound(jobId)
    case NomadError.Unreachable => InstanceError.NomadUnreachable
  }
}
