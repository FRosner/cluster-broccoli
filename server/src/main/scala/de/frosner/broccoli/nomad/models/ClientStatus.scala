package de.frosner.broccoli.nomad.models

import enumeratum._

import scala.collection.immutable

/**
  * The nomad client status.
  *
  * Presumably this is the status the allocation
  *
  * See https://github.com/hashicorp/nomad/blob/2e7d8adfa4e9cbbc85009943f79641ac55875aa6/nomad/structs/structs.go#L4260
  * for all possible values.
  */
sealed trait ClientStatus extends EnumEntry with EnumEntry.Lowercase

object ClientStatus extends Enum[ClientStatus] with PlayJsonEnum[ClientStatus] {

  override val values: immutable.IndexedSeq[ClientStatus] = findValues

  case object Pending extends ClientStatus
  case object Running extends ClientStatus
  case object Complete extends ClientStatus
  case object Failed extends ClientStatus
  case object Lost extends ClientStatus
}
