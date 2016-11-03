package de.frosner.broccoli.models

import de.frosner.broccoli.controllers.InstanceController
import de.frosner.broccoli.models.InstanceStatusJson.{instanceStatusReads, instanceStatusWrites}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class InstanceControllerSpec extends Specification {

  "Instance 'anonymization' should" should {

    "remove one secret parameter value" in {
      val originalInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> "noone knows"
          )
        ),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      val expectedInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> null
          )
        ),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      InstanceController.removeSecretVariables(originalInstance) === expectedInstance
    }

    "remove multiple secret parameter values" in {
      val originalInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              ),
              "id" -> ParameterInfo(
                name = "secret id",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> "noone knows"
          )
        ),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      val expectedInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              ),
              "id" -> ParameterInfo(
                name = "secret id",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> null,
            "password" -> null
          )
        ),
        status = InstanceStatus.Unknown,
        services = Map.empty
      )
      InstanceController.removeSecretVariables(originalInstance) === expectedInstance
    }

  }

}
