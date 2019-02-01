package de.frosner.broccoli.nomad

/**
  * The Nomad configuration.
  *
  * @param url The URL to connect to to access nomad via HTTP.
  */
final case class NomadConfiguration(url: String,
                                    tokenEnvName: String,
                                    namespacesEnabled: Boolean,
                                    namespaceVariable: String)
