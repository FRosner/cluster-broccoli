package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import de.frosner.broccoli.auth.Account
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class AccountSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {
  "The RemoveSecrets instance" should {
    "replace the password with an empty string" in prop { (account: Account) =>
      account.removeSecrets.password === ""
    }
  }
}
