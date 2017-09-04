package de.frosner.broccoli.auth

import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.services.IdentityService
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule

/**
  * Configure authentication for Broccoli.
  */
class AuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provides authentication configuration.
    *
    * @param config The Broccoli configuration
    * @return The authentication configuration of config
    */
  @Provides
  def providesAuthConfiguration(config: BroccoliConfiguration): AuthConfiguration = config.auth

  /**
    * Provides the identity service for Broccoli.
    *
    * @param config The configuration
    * @return The identity service
    */
  @Provides
  def providesIdentityService(config: AuthConfiguration): IdentityService[Account] = {
    val accounts = config.mode match {
      case AuthMode.None => Seq(Account.anonymous)
      case AuthMode.Conf =>
        config.conf.accounts.map(
          account => Account(account.username, account.password, account.instanceRegex, account.role)
        )
    }
    new InMemoryIdentityService(accounts)
  }
}
