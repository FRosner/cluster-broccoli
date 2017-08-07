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

  /**
    * Provide a nomad client.
    *
    * The nomad client provided by this method uses Play's client so we let it run on Play's default execution context.
    * Play's web service client does not block.
    *
    * @param config The nomad configuration
    * @param wsClient The play web service client to use
    * @return A HTTP client for Nomad
    */
  @Provides
  @Singleton
  def provideNomadClient(config: NomadConfiguration, wsClient: WSClient): NomadClient =
    new NomadHttpClient(config.url, wsClient)(play.api.libs.concurrent.Execution.defaultContext)
}
