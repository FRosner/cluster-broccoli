package de.frosner.broccoli.nomad

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import com.netaporter.uri.Uri
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
  * Provide bindings for Nomad access.
  */
class NomadModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provide Nomad configuration.
    *
    * @param config The whole broccoli configuration
    * @return The nomad part of that configuration
    */
  @Provides
  def provideNomadConfiguration(config: BroccoliConfiguration): NomadConfiguration = config.nomad

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
  def provideNomadClient(config: NomadConfiguration, wsClient: WSClient, context: ExecutionContext): NomadClient =
    new NomadHttpClient(Uri.parse(config.url), config.tokenEnvName, wsClient)(context)
}
