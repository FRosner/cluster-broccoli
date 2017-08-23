package de.frosner.broccoli.instances.storage

import de.frosner.broccoli.instances.storage.couchdb.CouchDBConfiguration
import de.frosner.broccoli.instances.storage.filesystem.FileSystemConfiguration

/**
  * Configuration specific to the instance storage type
  *
  * @param `type` The storage type to use. Can be either 'fs' or 'couchdb'
  * @param fs  Configuration for FileSystemInstanceStorage
  * @param couchdb Configuration for CouchDBInstanceStorage
  */
final case class StorageConfiguration(
    `type`: StorageType,
    fs: FileSystemConfiguration,
    couchdb: CouchDBConfiguration
)
