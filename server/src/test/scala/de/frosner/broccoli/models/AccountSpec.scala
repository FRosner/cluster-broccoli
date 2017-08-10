package de.frosner.broccoli.models

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps

class AccountSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {
  "The RemoveSecrets instance" should {
    "replace the password with an empty string" in prop { (account: Account) =>
      account.removeSecrets.password === ""
    }
  }
}
