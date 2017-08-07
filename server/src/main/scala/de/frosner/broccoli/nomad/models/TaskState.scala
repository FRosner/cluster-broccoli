package de.frosner.broccoli.nomad.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

/**
  * The state of a single task.
  *
  * See https://github.com/hashicorp/nomad/blob/2e7d8adfa4e9cbbc85009943f79641ac55875aa6/nomad/structs/structs.go#L3367
  * for the list of possible task states.
  */
sealed trait TaskState extends EnumEntry with EnumEntry.Lowercase

object TaskState extends Enum[TaskState] with PlayJsonEnum[TaskState] {
  override val values: immutable.IndexedSeq[TaskState] = findValues

  case object Pending extends TaskState
  case object Running extends TaskState
  case object Dead extends TaskState
}
