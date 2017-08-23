package de.frosner.broccoli.auth

import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import de.frosner.broccoli.models.ModelArbitraries
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class UserAccountSpec extends Specification with ScalaCheck with ModelArbitraries with ToRemoveSecretsOps {
  "The RemoveSecrets instance" should {
    "replace the password with an empty string" in prop { (account: UserAccount) =>
      account.removeSecrets.password === ""
    }
  }
}
