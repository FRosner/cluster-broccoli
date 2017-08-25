package de.frosner.broccoli.auth

import de.frosner.broccoli.RemoveSecrets
import play.api.libs.json.{Json, Writes}

/**
  * A user account
  *
  * @param name The account name
  * @param password The account password
  * @param instanceRegex The instance regex for access control to instances
  * @param role The user role
  */
case class UserAccount(name: String, password: String, instanceRegex: String, role: UserRole)

object UserAccount {

  /**
    * The anonymous user account to be used when authentication is disabled.
    */
  val anonymous: UserAccount = UserAccount(
    name = "anonymous",
    password = "",
    instanceRegex = ".*",
    role = UserRole.Administrator
  )

  implicit val accountWrites: Writes[UserAccount] = Writes { account =>
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
  implicit val accountRemoveSecrets: RemoveSecrets[UserAccount] = RemoveSecrets.instance(_.copy(password = ""))
}
