package de.frosner.broccoli.auth

/**
  * Configuration for authentication.
  *
  * @param mode The authentication mode
  * @param conf Configuration for conf mode
  */
final case class AuthConfiguration(mode: AuthMode, conf: AuthConfiguration.Conf)

object AuthConfiguration {

  /**
    * Configuration for the "conf" auth mode.
    *
    * @param accounts The list of known accounts
    */
  final case class Conf(accounts: Seq[Conf.Account])

  object Conf {

    /**
      * A configured user account.
      *
      * @param username The user name
      * @param password The password for authentication
      * @param instanceRegex The instance regex for access control to instances
      * @param role The user role
      */
    final case class Account(
        username: String,
        password: String,
        instanceRegex: String,
        role: UserRole
    )
  }
}
