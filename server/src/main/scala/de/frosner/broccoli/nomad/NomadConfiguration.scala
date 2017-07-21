package de.frosner.broccoli.nomad

import com.typesafe.config.Config

/**
  * The Nomad configuration.
  *
  * @param url The URL to connect to to access nomad via HTTP.
  */
final case class NomadConfiguration(url: String)

object NomadConfiguration {

  /**
    * Extract a nomad configuration from a typesafe config.
    *
    * @param config The config
    * @return The corresponding nomad configuration
    */
  def fromConfig(config: Config): NomadConfiguration = NomadConfiguration(config.getString("url"))
}
