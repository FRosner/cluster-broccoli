package de.frosner.broccoli.nomad.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

/**
  * The state of a single task.
  */
sealed trait TaskState extends EnumEntry with EnumEntry.Lowercase

object TaskState extends Enum[TaskState] with PlayJsonEnum[TaskState] {
  val values = findValues

  case object Dead extends TaskState
  case object Running extends TaskState
  case object Pending extends TaskState
}
