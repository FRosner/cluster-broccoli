package de.frosner.broccoli.auth

import scala.concurrent.duration.Duration

/**
  * Authentication configuration.
  *
  * @param mode The authentication mode
  * @param session Configuration for authenticated sessions
  * @param cookie Configuration for authentication cookies
  * @param conf Configuration for conf authentication mode.
  * @param allowedFailedLogins How many failed logins are allowed
  */
final case class AuthConfiguration(
    mode: AuthMode,
    session: AuthConfiguration.Session,
    cookie: AuthConfiguration.Cookie,
    conf: AuthConfiguration.Conf,
    allowedFailedLogins: Int
)

object AuthConfiguration {

  /**
    * @param accounts The list of known accounts
    */
  final case class Conf(accounts: List[ConfAccount])

  /**
    * @param timeout Timeout until automatic logout
    * @param allowMultiLogin Whether a user can login multiple times
    */
  final case class Session(timeout: Duration, allowMultiLogin: Boolean)

  final case class Cookie(secure: Boolean)

  /**
    * A configured user account.
    *
    * @param username The username
    * @param password The password
    * @param instanceRegex The instance regex for the account
    * @param role The account role
    */
  final case class ConfAccount(username: String, password: String, instanceRegex: String, role: Role)
}
