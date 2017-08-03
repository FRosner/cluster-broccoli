package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import de.frosner.broccoli.instances.StorageType

/**
  * Configuration specific to the instance storage type
  *
  * @param storageType The storage type to use. Can be either 'fs' or 'couchdb'
  * @param fsConfig  Configuration for FileSystemInstanceStorage
  * @param couchDBConfig Configuration for CouchDBInstanceStorage
  */
final case class InstanceStorageConfiguration(storageType: StorageType,
                                              fsConfig: FileSystemInstanceStorageConfiguration,
                                              couchDBConfig: CouchDBInstanceStorageConfiguration)

object InstanceStorageConfiguration {
  def fromConfig(config: Config): InstanceStorageConfiguration =
    StorageType.withNameOption(config.getString("type")) match {
      case Some(storageType) =>
        InstanceStorageConfiguration(
          storageType,
          FileSystemInstanceStorageConfiguration.fromConfig(config.getConfig("fs")),
          CouchDBInstanceStorageConfiguration.fromConfig(config.getConfig("couchdb"))
        )
      case None =>
        throw new IllegalArgumentException(
          s"broccoli.instances.storage.type=${config.getString("type")} is invalid. Only ${StorageType.values.mkString(", ")} supported.")
    }
}
