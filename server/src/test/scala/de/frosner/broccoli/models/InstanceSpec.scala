package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class InstanceSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {

  "The RemoveSecrets instance" should {
    "remove secret instance parameters" in prop { (instance: Instance) =>
      val publicInstance = instance.removeSecrets
      val (secret, public) = publicInstance.parameterValues.partition {
        case (id, _) =>
          instance.template.parameterInfos(id).secret.getOrElse(false)
      }
      (secret.values must contain(beNull[ParameterValue]).foreach) and (public.values must contain(
        not(beNull[ParameterValue])).foreach)
    }
  }

  "An instance" should {
    "be possible to construct if the parameters to be filled match the ones in the template's parameter infos" in {
      val parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
      val instance1 =
        Instance("1", Template("1", "\"{{id}}\"", "desc", "#doc-url", parameterInfos), Map("id" -> RawParameterValue("Heinz")))
      val instance2 =
        Instance("1", Template("1", "\"{{id}}\"", "desc", "#doc-url",parameterInfos), Map("id" -> RawParameterValue("Heinz")))
      instance1 === instance2
    }

    "throw an exception during construction if not all variables are specified in the template's parameter infos" in {
      Instance("1", Template("1", "\"{{id}}\"", "desc", "#doc-url",Map.empty), Map("id" -> RawParameterValue("Heinz"))) must throwA(
        new IllegalArgumentException(
          "requirement failed: The given parameters values (Set(id)) need to match the ones in the template (Set()) (instance id 1)."))
    }
  }

}
