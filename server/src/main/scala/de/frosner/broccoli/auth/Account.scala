package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.Identity
import play.api.libs.json.{Json, OFormat}

/**
  * A user account in Broccoli.
  *
  * @param name The account name
  * @param instanceRegex A regex matching instance names this account is allowed to access
  * @param role The role of the user
  */
final case class Account(name: String, instanceRegex: String, role: Role) extends Identity

object Account {
  implicit val accountFormat: OFormat[Account] = Json.format[Account]

  /**
    * The anonymous user.
    */
  val anonymous = Account(
    name = "anonymous",
    instanceRegex = ".*",
    role = Role.Administrator
  )
}
