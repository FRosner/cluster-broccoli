package de.frosner.broccoli.nomad

import com.typesafe.config.Config

/**
  * The Nomad configuration.
  *
  * @param url The URL to connect to to access nomad via HTTP.
  */
final case class NomadConfiguration(url: String)
