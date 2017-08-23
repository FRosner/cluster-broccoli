package de.frosner.broccoli.instances.storage.couchdb

/**
  * Configuration for couchdb instance storage.
  *
  * @param url database address
  * @param database database name where the instance information will be stored
  */
final case class CouchDBConfiguration(url: String, database: String)
