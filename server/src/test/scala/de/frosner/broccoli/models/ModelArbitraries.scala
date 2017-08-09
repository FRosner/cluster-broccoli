package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import org.scalacheck.{Arbitrary, Gen}

/**
  * Scalacheck arbitrary instances for Broccoli models.
  */
trait ModelArbitraries {

  implicit val arbitraryRole: Arbitrary[Role] = Arbitrary(Gen.oneOf(Role.values))

  implicit def arbitraryAccount(implicit arbRole: Arbitrary[Role]): Arbitrary[Account] = Arbitrary {
    for {
      name <- Gen.identifier.label("name")
      password <- Gen.identifier.label("password")
      instanceRegex <- Gen.identifier.label("instanceRegex")
      role <- arbRole.arbitrary.label("role")
    } yield UserAccount(name, password, instanceRegex, role)
  }

  implicit def arbitraryInstanceError(implicit arbRole: Arbitrary[Role]): Arbitrary[InstanceError] =
    Arbitrary(
      Gen.oneOf(
        Gen
          .zip(Gen.identifier.label("instanceId"),
               Gen.option(Gen.identifier.label("message").map(m => new Throwable(m))))
          .map(InstanceError.NotFound.tupled),
        Gen.const(InstanceError.IdMissing),
        Gen.identifier.label("templateId").map(InstanceError.TemplateNotFound),
        Gen.identifier.label("reason").map(InstanceError.InvalidParameters),
        Gen
          .zip(Gen.identifier.label("instanceId"), Gen.identifier.label("regex"))
          .map(InstanceError.UserRegexDenied.tupled),
        Gen.nonEmptyBuildableOf[Set[Role], Role](arbRole.arbitrary).map(InstanceError.RolesRequired(_)),
        Gen.identifier.label("message").map(message => InstanceError.Generic(new Throwable(message)))
      ))

  implicit def arbitraryTaskAllocation(
      implicit arbClientStatus: Arbitrary[ClientStatus],
      arbTaskState: Arbitrary[TaskState]
  ): Arbitrary[Task.Allocation] =
    Arbitrary {
      for {
        id <- Gen.uuid
        clientStatus <- arbClientStatus.arbitrary
        taskState <- arbTaskState.arbitrary
      } yield Task.Allocation(id.toString, clientStatus, taskState)
    }

  implicit def arbitraryTask(implicit arbAllocation: Arbitrary[Task.Allocation]): Arbitrary[Task] = Arbitrary {
    for {
      name <- Gen.identifier.label("name")
      allocations <- Gen.listOf(arbAllocation.arbitrary)
    } yield Task(name, allocations)
  }
}

/**
  * Import object for arbitrary instances of models.
  */
object arbitraries extends ModelArbitraries
