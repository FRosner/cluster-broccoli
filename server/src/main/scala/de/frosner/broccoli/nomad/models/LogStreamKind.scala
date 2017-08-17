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

  /**
    * Use log stream kinds in Play route paths.
    */
  implicit def logStreamKindPathBindable(implicit stringBinder: PathBindable[String]): PathBindable[LogStreamKind] =
    new PathBindable[LogStreamKind] {
      override def bind(key: String, value: String): Either[String, LogStreamKind] =
        stringBinder.bind(key, value).flatMap(LogStreamKind.withNameOption(_).toRight("Invalid log kind"))

      override def unbind(_key: String, value: LogStreamKind): String = value.entryName
    }
}
