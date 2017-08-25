package de.frosner.broccoli.auth

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.codingwell.scalaguice.ScalaModule

/**
  * A module to disable all authentication.
  */
class NoAuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    // Do not provide any auth info, but still bind a password hasher and an auth info repository to be able to inject
    // a CredentialsProviders
    bind[PasswordHasherRegistry].toInstance(PasswordHasherRegistry(NoPasswordHasher))
    bind[AuthInfoRepository].toInstance(new DelegableAuthInfoRepository())
  }

}
