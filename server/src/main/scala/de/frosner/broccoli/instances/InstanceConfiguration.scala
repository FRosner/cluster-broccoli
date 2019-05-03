package de.frosner.broccoli.instances

import de.frosner.broccoli.instances.storage.StorageConfiguration

/**
  * Instance Configuration
  *
  * @param storage Configuration specific to the instance storage type
  */
final case class InstanceConfiguration(storage: StorageConfiguration)
