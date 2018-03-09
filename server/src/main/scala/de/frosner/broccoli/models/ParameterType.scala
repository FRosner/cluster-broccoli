package de.frosner.broccoli.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

sealed trait ParameterType extends EnumEntry with EnumEntry.Lowercase

object ParameterType extends Enum[ParameterType] with PlayJsonEnum[ParameterType] {
  val a: Boolean = true
  val values: immutable.IndexedSeq[ParameterType] = findValues
  case object Raw extends ParameterType
  case object String extends ParameterType
  case object Numeric extends ParameterType
}
