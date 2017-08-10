package de.frosner.broccoli.models

import de.frosner.broccoli.RemoveSecrets
import play.api.libs.json.{Json, Writes}

sealed trait Credentials {

  val name: String

  val password: String

}

case class UserCredentials(name: String, password: String) extends Credentials

sealed trait Account extends Credentials {

  val name: String

  val password: String

  val instanceRegex: String

  val role: Role

  override def toString: String = s"$name ($role)"

}

object Account {
  implicit val accountWrites: Writes[Account] = Writes { account =>
    Json.obj(
      "name" -> account.name,
      "instanceRegex" -> account.instanceRegex,
      "role" -> account.role
    )
  }

  /**
    * Remove secrets from an account.
    *
    * The instance returns a new UserAccount with the password replaced with an empty string.
    */
  implicit val accountRemoveSecrets: RemoveSecrets[Account] = RemoveSecrets.instance(
    account =>
      UserAccount(
        name = account.name,
        password = "",
        instanceRegex = account.instanceRegex,
        role = account.role
    ))
}

case class UserAccount(name: String, password: String, instanceRegex: String, role: Role) extends Account

case object Anonymous extends Account {

  val name = "anonymous"

  val password = ""

  val instanceRegex = ".*"

  val role = Role.Administrator

}
