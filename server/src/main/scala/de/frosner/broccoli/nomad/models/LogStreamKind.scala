package de.frosner.broccoli.nomad.models

import enumeratum._
import cats.syntax.either._
import play.api.mvc.PathBindable

import scala.collection.immutable

/**
  * The kind of log stream of a client.
  */
sealed trait LogStreamKind extends EnumEntry with EnumEntry.Lowercase

object LogStreamKind extends Enum[LogStreamKind] {

  override def values: immutable.IndexedSeq[LogStreamKind] = findValues

  /**
    * The standard output stream of a task.
    */
  case object StdOut extends LogStreamKind

  /**
    * The standard error stream of a task.
    */
  case object StdErr extends LogStreamKind
}
