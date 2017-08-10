package de.frosner.broccoli.nomad

import de.frosner.broccoli.nomad.models._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Arbitrary instances for Nomad models.
  */
trait ModelArbitraries {
  implicit val arbitraryClientStatus: Arbitrary[ClientStatus] = Arbitrary(Gen.oneOf(ClientStatus.values))

  implicit val arbitraryTaskState: Arbitrary[TaskState] = Arbitrary(Gen.oneOf(TaskState.values))
}

/**
  * Import object for Nomad arbitraries.
  */
object arbitraries extends ModelArbitraries
