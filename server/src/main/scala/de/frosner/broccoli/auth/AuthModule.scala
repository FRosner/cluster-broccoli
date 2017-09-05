package de.frosner.broccoli.auth

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.InMemoryAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.{ExecutionContext, Future}

/**
  * Configure authentication for Broccoli.
  */
class AuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit =
    bind[PasswordHasherRegistry].toInstance(PasswordHasherRegistry(new BCryptPasswordHasher()))

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
          account => Account(account.username, account.instanceRegex, account.role)
        )
    }
    new InMemoryIdentityService(accounts)
  }

  /**
    * Provides the authentication info repository.
    *
    * If authentication mode is conf create an in-memory DAO for password info and add all accounts from configuration
    * to it.  Hash all passwords with bcrypt to slow-down login and make password-cracking attacks less feasible.
    *
    * @param config The authentication configuration
    * @param passwordHasherRegistry The current and deprecated password hashers.
    * @return An authentication info repository.
    */
  @Provides
  @Singleton
  def providesAuthInfoRepository(
      config: AuthConfiguration,
      passwordHasherRegistry: PasswordHasherRegistry
  )(implicit ec: ExecutionContext): AuthInfoRepository =
    new DelegableAuthInfoRepository(config.mode match {
      case AuthMode.Conf =>
        // Create an in-memory DAO and add all accounts from configuration to it
        val dao = new InMemoryAuthInfoDAO[PasswordInfo]()
        config.conf.accounts.foreach { account =>
          dao.add(LoginInfo(CredentialsProvider.ID, account.username),
                  passwordHasherRegistry.current.hash(account.password))
        }
        dao
      case _ =>
        // Use an empty DAO
        new InMemoryAuthInfoDAO[PasswordInfo]()
    })
}
