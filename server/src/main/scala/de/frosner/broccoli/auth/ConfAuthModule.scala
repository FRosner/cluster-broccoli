package de.frosner.broccoli.auth

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

/**
  * A module to authenticate against a list of accounts in the application configuration.
  */
class ConfAuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit =
    bind[PasswordHasherRegistry].toInstance(PasswordHasherRegistry(NoPasswordHasher))

  @Provides
  @Singleton
  def provideAuthConfiguration(configuration: BroccoliConfiguration): AuthConfiguration = configuration.auth

  /**
    * Provides a DAO to access passwords from the configuration.
    *
    * @param configuration The configuration
    * @param passwordHasherRegistry The hasher registry to hash passwords for in-memory storage.
    */
  @Provides
  @Singleton
  def provideConstantPasswordInfoDAO(
      configuration: AuthConfiguration,
      passwordHasherRegistry: PasswordHasherRegistry
  ): ConstantPasswordInfoDAO =
    new ConstantPasswordInfoDAO(
      configuration.conf.accounts
        .map(account => account.username -> passwordHasherRegistry.current.hash(account.password))
        .toMap)

  /**
    * Provides the repository for authentication information.
    *
    * @param passwordInfoDAO The DAO to get passwords from the configuration
    * @return The auth info repository
    */
  @Provides
  @Singleton
  def provideAuthInfoRepository(
      passwordInfoDAO: ConstantPasswordInfoDAO,
      executionContext: ExecutionContext
  ): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordInfoDAO)(executionContext)
}
