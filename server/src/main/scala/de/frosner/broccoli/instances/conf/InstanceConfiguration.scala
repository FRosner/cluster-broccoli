package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import de.frosner.broccoli.models.ParameterType
import play.api.Logger

/**
  * Instance Configuration
  *
  * @param defaultParameterType The parameter type to be used by default for the instance parameters
  * @param storageConfiguration Configuration specific to the instance storage type
  */
final case class InstanceConfiguration(defaultParameterType: ParameterType,
                                       storageConfiguration: InstanceStorageConfiguration)

object InstanceConfiguration {
  private val log = Logger(getClass)

  def fromConfig(config: Config): InstanceConfiguration = {
    val defaultParameterType = ParameterType.withName(config.getString("parameters.defaultType"))

    val storageConfiguration = InstanceStorageConfiguration.fromConfig(config.getConfig("storage"))
    InstanceConfiguration(defaultParameterType, storageConfiguration)
  }
}
