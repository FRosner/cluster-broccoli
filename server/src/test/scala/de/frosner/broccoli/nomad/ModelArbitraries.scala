package de.frosner.broccoli.nomad

import de.frosner.broccoli.nomad.models._
import org.scalacheck.{Arbitrary, Gen}
import shapeless.tag
import squants.information.Bytes
import squants.time.Megahertz

/**
  * Arbitrary instances for Nomad models.
  */
trait ModelArbitraries {
  implicit val arbitraryClientStatus: Arbitrary[ClientStatus] = Arbitrary(Gen.oneOf(ClientStatus.values))

  implicit val arbitraryTaskState: Arbitrary[TaskState] = Arbitrary(Gen.oneOf(TaskState.values))

  implicit val arbitraryTaskStateEvents: Arbitrary[TaskStateEvents] = Arbitrary(for {
    taskState <- arbitraryTaskState.arbitrary
  } yield TaskStateEvents(taskState))

  implicit val arbitraryAllocation: Arbitrary[Allocation] = Arbitrary(for {
    id <- Gen.identifier.label("id").map(tag[Allocation.Id](_))
    jobId <- Gen.identifier.label("jobId").map(tag[Job.Id](_))
    nodeId <- Gen.identifier.label("nodeId").map(tag[Node.Id](_))
    clientStatus <- arbitraryClientStatus.arbitrary
    tasks <- Gen.mapOf(
      Gen.zip(
        Gen.identifier.label("taskName").map(tag[Task.Name](_)),
        arbitraryTaskStateEvents.arbitrary
      ))
  } yield Allocation(id, jobId, nodeId, clientStatus, tasks))

  implicit val arbitraryCpuStatus: Arbitrary[CpuStats] = Arbitrary(for {
    ticks <- Gen
      .chooseNum[Double](0, 10000)
      .map(ticks => tag[CpuStats.TotalTicks](Megahertz(ticks)))
      .label("totalTicks")
  } yield CpuStats(ticks))

  implicit val arbitraryMemoryStats: Arbitrary[MemoryStats] = Arbitrary(for {
    rss <- Gen.chooseNum[Int](0, Int.MaxValue).label("RSS").map(bytes => tag[MemoryStats.RSS](Bytes(bytes)))
  } yield MemoryStats(rss))

  implicit def arbitraryResourceUsage(
      implicit arbCpuStats: Arbitrary[CpuStats],
      arbMemoryStats: Arbitrary[MemoryStats]
  ): Arbitrary[ResourceUsage] =
    Arbitrary(for {
      cpu <- arbCpuStats.arbitrary
      memory <- arbMemoryStats.arbitrary
    } yield ResourceUsage(cpu, memory))

  implicit val arbitraryNomadError: Arbitrary[NomadError] = Arbitrary(
    Gen.oneOf(NomadError.Unreachable, NomadError.NotFound))
}

/**
  * Import object for Nomad arbitraries.
  */
object arbitraries extends ModelArbitraries
