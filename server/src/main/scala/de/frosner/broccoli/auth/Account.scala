package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.Identity
import de.frosner.broccoli.RemoveSecrets
import play.api.libs.json.{Json, Writes}

/**
  * A user account in Broccoli.
  *
  * @param name The account name
  * @param password The account password
  * @param instanceRegex A regex matching instance names this account is allowed to access
  * @param role The role of the user
  */
case class Account(name: String, password: String, instanceRegex: String, role: Role) extends Identity

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
    * The instance returns a new Account with the password replaced with an empty string.
    */
  implicit val accountRemoveSecrets: RemoveSecrets[Account] = RemoveSecrets.instance(_.copy(password = ""))

  /**
    * The anonymous user.
    */
  val anonymous = Account(
    name = "anonymous",
    password = "",
    instanceRegex = ".*",
    role = Role.Administrator
  )
}
