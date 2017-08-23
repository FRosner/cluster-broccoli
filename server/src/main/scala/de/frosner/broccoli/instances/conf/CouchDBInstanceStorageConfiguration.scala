package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import play.api.Logger

/**
  * Configuration for CouchDBInstanceStorage
  *
  * @param url database address
  * @param database database name where the instance information will be stored
  */
final case class CouchDBInstanceStorageConfiguration(url: String, database: String)
