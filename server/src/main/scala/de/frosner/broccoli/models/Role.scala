package de.frosner.broccoli.models

import enumeratum._

import scala.collection.immutable

/**
  * A user role in the application
  */
sealed trait Role extends EnumEntry with EnumEntry.Lowercase

object Role extends Enum[Role] with PlayJsonEnum[Role] {
  override def values: immutable.IndexedSeq[Role] = findValues

  /**
    * Grants all privileges.
    */
  final case object Administrator extends Role

  /**
    * Grants limited privileges to edit instances.
    */
  final case object Operator extends Role

  /**
    * Grants use of instances but does not allow modification.
    */
  final case object User extends Role
}
