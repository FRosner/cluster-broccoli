package de.frosner.broccoli.models

import org.specs2.mutable.Specification

import org.specs2.mutable._

class InstanceSpec extends Specification {

  "An instance" should {

    "be possible to construct if the parameters to be filled match the ones in the template" in {
      val instance1 = Instance("1", Template("1", "${name}"), Map("name" -> "Heinz"))
      val instance2 = Instance("1", Template("1", "${name}"), Map("name" -> "Heinz"))
      instance1 === instance2
    }

    "check that the parameters to be filled are the same ones as in the template" in {
      Instance("1", Template("1", "${name}"), Map.empty) must throwA[IllegalArgumentException]
    }

  }

}
