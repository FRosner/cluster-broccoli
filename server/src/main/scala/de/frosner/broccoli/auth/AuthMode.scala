package de.frosner.broccoli.auth

import enumeratum.EnumEntry.Lowercase
import enumeratum._

import scala.collection.immutable

/**
  * Authentication mode for Broccoli
  */
sealed trait AuthMode extends EnumEntry with Lowercase

object AuthMode extends Enum[AuthMode] {
  override val values: immutable.IndexedSeq[AuthMode] = findValues

  final case object None extends AuthMode

  final case object Conf extends AuthMode
}
