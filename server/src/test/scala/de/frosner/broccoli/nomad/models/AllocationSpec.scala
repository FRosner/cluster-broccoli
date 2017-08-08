package de.frosner.broccoli.nomad.models

import de.frosner.broccoli.util.Resources
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class AllocationSpec extends Specification {

  "Allocation" should {

    "decode from JSON" in {
      val allocations = Json
        .parse(Resources.readAsString("/de/frosner/broccoli/services/nomad/allocations.json"))
        .validate[List[Allocation]]
        .asEither

      val allocation = Allocation(
        id = "520bc6c3-53c9-fd2e-5bea-7d0b9dbef254",
        nodeId = "cf3338e9-5ed0-88ef-df7b-9dd9708130c8",
        clientStatus = ClientStatus.Running,
        taskStates = Map("http-task" -> TaskStateEvents(TaskState.Running))
      )
      allocations should beRight(List(allocation))
    }
  }
}
