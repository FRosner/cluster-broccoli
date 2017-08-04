package de.frosner.broccoli.instances

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

/**
  * Type of the instance storage
  *
  * @param entryName type name used in the Play configuration
  */
sealed abstract class StorageType(override val entryName: String) extends EnumEntry

object StorageType extends Enum[StorageType] with PlayJsonEnum[StorageType] {
  val values: immutable.IndexedSeq[StorageType] = findValues
  case object FileSystem extends StorageType("fs")
  case object CouchDB extends StorageType("couchdb")
}
