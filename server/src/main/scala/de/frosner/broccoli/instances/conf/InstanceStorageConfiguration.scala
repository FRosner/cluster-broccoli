package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import de.frosner.broccoli.instances.StorageType

/**
  * Configuration specific to the instance storage type
  *
  * @param `type` The storage type to use. Can be either 'fs' or 'couchdb'
  * @param fs  Configuration for FileSystemInstanceStorage
  * @param couchdb Configuration for CouchDBInstanceStorage
  */
final case class InstanceStorageConfiguration(
    `type`: StorageType,
    fs: FileSystemInstanceStorageConfiguration,
    couchdb: CouchDBInstanceStorageConfiguration
)
