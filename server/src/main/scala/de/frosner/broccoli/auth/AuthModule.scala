package de.frosner.broccoli.auth

import com.google.inject.{AbstractModule, Provides}
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
}
