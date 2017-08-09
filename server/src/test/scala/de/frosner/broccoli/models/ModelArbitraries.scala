package de.frosner.broccoli.models

import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import org.scalacheck.{Arbitrary, Gen}

/**
  * Scalacheck arbitrary instances for Broccoli models.
  */
trait ModelArbitraries {

  implicit val arbitraryRole: Arbitrary[Role] = Arbitrary(Gen.oneOf(Role.values))

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
}

/**
  * Import object for arbitrary instances of models.
  */
object arbitraries extends ModelArbitraries
