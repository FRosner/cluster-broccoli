package de.frosner.broccoli.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

sealed trait TemplateFormat extends EnumEntry with EnumEntry.Lowercase

object TemplateFormat extends Enum[TemplateFormat] with PlayJsonEnum[TemplateFormat] {

  override val values: immutable.IndexedSeq[TemplateFormat] = findValues

  case object JSON extends TemplateFormat
  case object HCL extends TemplateFormat
}
