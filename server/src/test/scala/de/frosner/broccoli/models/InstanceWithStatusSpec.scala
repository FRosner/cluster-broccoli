package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class InstanceWithStatusSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {
  "Instance RemoveSecrets should" should {

    "remove secrets from the contained instance" in prop { (instance: InstanceWithStatus) =>
      instance.removeSecrets === instance.copy(instance = instance.instance.removeSecrets)
    }

    "remove one secret parameter value" in {
      val originalInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "id" -> ParameterInfo("id", None, None, None, None, None),
              "password" -> ParameterInfo(
                id = "secret password",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              )
            )
          ),
          parameterValues = Map(
            "id" -> StringParameterValue("i"),
            "password" -> StringParameterValue("noone knows")
          )
        ),
        status = JobStatus.Unknown,
        services = Seq.empty,
        periodicRuns = Seq.empty
      )
      val expectedInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "id" -> ParameterInfo("id", None, None, None, None, None),
              "password" -> ParameterInfo(
                id = "secret password",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              )
            )
          ),
          parameterValues = Map(
            "id" -> StringParameterValue("i"),
            "password" -> null
          )
        ),
        status = JobStatus.Unknown,
        services = Seq.empty,
        periodicRuns = Seq.empty
      )
      originalInstance.removeSecrets === expectedInstance
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
              "id" -> ParameterInfo("id", None, None, None, None, None),
              "password" -> ParameterInfo(
                id = "secret password",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              ),
              "id" -> ParameterInfo(
                id = "secret id",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              )
            )
          ),
          parameterValues = Map(
            "id" -> StringParameterValue("i"),
            "password" -> StringParameterValue("noone knows")
          )
        ),
        status = JobStatus.Unknown,
        services = Seq.empty,
        periodicRuns = Seq.empty
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
                id = "secret password",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              ),
              "id" -> ParameterInfo(
                id = "secret id",
                name = None,
                default = None,
                secret = Some(true),
                `type` = ParameterType.String,
                orderIndex = None
              )
            )
          ),
          parameterValues = Map(
            "id" -> null,
            "password" -> null
          )
        ),
        status = JobStatus.Unknown,
        services = Seq.empty,
        periodicRuns = Seq.empty
      )
      originalInstance.removeSecrets === expectedInstance
    }

  }
}
