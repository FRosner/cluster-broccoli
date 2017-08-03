package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import de.frosner.broccoli.models.ParameterType

/**
  * Instance Configuration
  *
  * @param pollingFrequency Integer (seconds) to control the time between asking Nomad and Consul for job and service status
  * @param defaultParameterType The parameter type to be used by default for the instance parameters
  * @param storageConfiguration Configuration specific to the instance storage type
  */
final case class InstanceConfiguration(pollingFrequency: Long,
                                       defaultParameterType: ParameterType,
                                       storageConfiguration: InstanceStorageConfiguration)

object InstanceConfiguration {
  protected val log = play.api.Logger(getClass)

  def fromConfig(config: Config): InstanceConfiguration = {
    val pollingFrequency = config.getLong("polling.frequency")
    if (pollingFrequency <= 0) {
      throw new IllegalArgumentException(
        s"Invalid polling frequency specified: $pollingFrequency. Needs to be a positive integer.")
    }

    log.info(s"Nomad/Consul polling frequency set to $pollingFrequency seconds")

    val defaultParameterType = ParameterType.withName(config.getString("parameters.defaultType"))

    val storageConfiguration = InstanceStorageConfiguration.fromConfig(config.getConfig("storage"))
    InstanceConfiguration(pollingFrequency, defaultParameterType, storageConfiguration)
  }
}
