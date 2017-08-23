package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import de.frosner.broccoli.instances.conf.InstanceConfiguration.Parameters
import de.frosner.broccoli.models.ParameterType
import play.api.Logger

/**
  * Instance Configuration
  *
  * @param parameters Configuration for parameters
  * @param storage Configuration specific to the instance storage type
  */
final case class InstanceConfiguration(parameters: Parameters, storage: InstanceStorageConfiguration)

object InstanceConfiguration {
  final case class Parameters(defaultType: ParameterType)
}
