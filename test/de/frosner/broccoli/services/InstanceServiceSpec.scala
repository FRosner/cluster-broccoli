package de.frosner.broccoli.services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import de.frosner.broccoli.models.{Instance, InstanceStatus, ParameterInfo, Template}
import org.specs2.mutable.Specification

class InstanceServiceSpec extends Specification {

  "Instance persistance" should {

    "work correctly" in {
      val instanceOut = new ByteArrayOutputStream()
      val originalInstances = Map(
        "1" -> Instance(
          id = "1",
          template = Template(
            id = "1",
            template = "{{id}}",
            description = "",
            parameterInfos = Map.empty
          ),
          parameterValues = Map("id" -> "1"),
          status = InstanceStatus.Unknown,
          services = Map.empty
        ),
        "2" -> Instance(
          id = "2",
          template = Template(
            id = "2",
            template = "{{id}} {{id}}",
            description = "",
            parameterInfos = Map.empty
          ),
          parameterValues = Map("id" -> "a"),
          status = InstanceStatus.Unknown,
          services = Map.empty
        )
      )
      InstanceService.persistInstances(originalInstances, instanceOut)
      val loadedInstances = InstanceService.loadInstances(new ByteArrayInputStream(instanceOut.toByteArray))
      loadedInstances.get === originalInstances
    }

  }

}
