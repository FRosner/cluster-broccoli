package de.frosner.broccoli.instances

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class StorageType(override val entryName: String) extends EnumEntry with EnumEntry.Lowercase

object StorageType extends Enum[StorageType] with PlayJsonEnum[StorageType] {
  val values = findValues
  case object FileSystem extends StorageType("fs")
  case object CouchDB extends StorageType("couchdb")
}
