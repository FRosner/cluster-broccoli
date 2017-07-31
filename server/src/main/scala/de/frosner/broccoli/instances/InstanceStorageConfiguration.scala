package de.frosner.broccoli.instances

import com.typesafe.config.Config

final case class InstanceStorageConfiguration(storageType: String,
                                              fsConfig: FileSystemInstanceStorageConfiguration,
                                              couchDBConfig: CouchDBInstanceStorageConfiguration)

object InstanceStorageConfiguration {
  def fromConfig(config: Config): InstanceStorageConfiguration = {
    val storageType = config.getString("type")
    val allowedStorageTypes = Set("fs", "couchdb")
    if (!allowedStorageTypes.contains(storageType)) {
      throw new IllegalArgumentException(
        s"broccoli.instances.storage.type=$storageType is invalid. Only ${allowedStorageTypes.mkString(", ")} supported.")
    }

    InstanceStorageConfiguration(
      storageType,
      FileSystemInstanceStorageConfiguration.fromConfig(config.getConfig("fs")),
      CouchDBInstanceStorageConfiguration.fromConfig(config.getConfig("couchdb"))
    )
  }
}
