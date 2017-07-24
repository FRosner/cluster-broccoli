package de.frosner.broccoli.nomad

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient

/**
  * Provide bindings for Nomad access.
  */
class NomadModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provide the nomad configuration.
    *
    * @param config The Play configuration
    * @return The nomad configuration extracted from the Play configuration
    */
  @Provides
  @Singleton
  def provideNomadConfiguration(config: Configuration): NomadConfiguration =
    NomadConfiguration.fromConfig(config.underlying.getConfig("broccoli.nomad"))
}
