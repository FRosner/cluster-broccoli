package de.frosner.broccoli.nomad.models

import shapeless.tag.@@

/**
  * The log of a task.
  *
  * @param kind The kind of log
  * @param contents The log
  */
final case class TaskLog(kind: LogStreamKind, contents: String @@ TaskLog.Contents)

object TaskLog {
  sealed trait Offset
  sealed trait Contents
}
