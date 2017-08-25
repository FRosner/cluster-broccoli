package de.frosner.broccoli.auth

import enumeratum._

import scala.collection.immutable

/**
  * Authentication mode for Broccoli
  */
sealed trait AuthMode extends EnumEntry

object AuthMode extends Enum[AuthMode] {
  override val values: immutable.IndexedSeq[AuthMode] = findValues

  /**
    * No authentication.
    */
  final case object None extends AuthMode with EnumEntry.Lowercase

  /**
    * Authenticate accounts a list of accounts from application configuration.
    */
  final case object Configuration extends AuthMode {
    override val entryName: String = "conf"
  }
}
