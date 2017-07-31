package de.frosner.broccoli.instances

import com.typesafe.config.Config

final case class InstanceConfiguration(pollingFrequency: Int, storageConfiguration: InstanceStorageConfiguration)

object InstanceConfiguration {
  protected val log = play.api.Logger(getClass)

  def fromConfig(config: Config): InstanceConfiguration = {
    val pollingFrequency = config.getInt("pollingFrequency")
    if (pollingFrequency <= 0) {
      throw new IllegalArgumentException(
        s"Invalid polling frequency specified: $pollingFrequency. Needs to be a positive integer.")
    }

    log.info(s"Nomad/Consul polling frequency set to $pollingFrequency seconds")

    val storageConfiguration = InstanceStorageConfiguration.fromConfig(config.getConfig("storage"))
    InstanceConfiguration(pollingFrequency, storageConfiguration)
  }
}
