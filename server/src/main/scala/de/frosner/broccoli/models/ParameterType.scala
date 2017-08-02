package de.frosner.broccoli.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait ParameterType extends EnumEntry with EnumEntry.Lowercase

object ParameterType extends Enum[ParameterType] with PlayJsonEnum[ParameterType] {
  val values = findValues
  case object Raw extends ParameterType
  case object String extends ParameterType
}
