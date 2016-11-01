package de.frosner.broccoli.services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import de.frosner.broccoli.models.{Instance, InstanceStatus, ParameterInfo, Template}
import org.specs2.mutable.Specification

class InstanceServiceSpec extends Specification {

  "Instance persistance" should {

    "work correctly" in {
      val instanceOut = new ByteArrayOutputStream()
      val originalInstance = Instance(
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
      )
      InstanceService.persistInstance(originalInstance, instanceOut)
      val loadedInstance = InstanceService.loadInstance(new ByteArrayInputStream(instanceOut.toByteArray))
      loadedInstance.get === originalInstance
    }

    "fail if the instance format is broken" in {
      val instanceOut = new ByteArrayOutputStream()
      val originalInstance = Instance(
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
      )
      InstanceService.persistInstance(originalInstance, instanceOut)
      val loadedInstance = InstanceService.loadInstance(new ByteArrayInputStream(instanceOut.toByteArray.drop(10)))
      loadedInstance.isFailure === true
    }

    "create the correct file name for an instance" in {
      val instance = Instance(
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
      )
      InstanceService.instanceToFileName(instance) === "1.json"
    }

  }

}
