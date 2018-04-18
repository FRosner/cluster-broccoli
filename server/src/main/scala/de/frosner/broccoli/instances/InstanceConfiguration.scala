package de.frosner.broccoli.instances

import de.frosner.broccoli.instances.storage.StorageConfiguration
import de.frosner.broccoli.models.ParameterType

/**
  * Instance Configuration
  *
  * @param storage Configuration specific to the instance storage type
  */
final case class InstanceConfiguration(storage: StorageConfiguration)
