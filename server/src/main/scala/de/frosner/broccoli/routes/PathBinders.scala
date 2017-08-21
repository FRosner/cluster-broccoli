package de.frosner.broccoli.routes

import cats.syntax.either._
import de.frosner.broccoli.nomad.models.LogStreamKind
import play.api.mvc.PathBindable
import squants.information.Information

/**
  * Bind values to route paths.
  */
trait PathBinders {

  /**
    * Use log stream kinds in Play route paths.
    */
  implicit def logStreamKindPathBindable(implicit stringBinder: PathBindable[String]): PathBindable[LogStreamKind] =
    new PathBindable[LogStreamKind] {
      override def bind(key: String, value: String): Either[String, LogStreamKind] =
        for {
          boundValue <- stringBinder.bind(key, value)
          kind <- LogStreamKind.withNameOption(boundValue).toRight("Invalid log kind")
        } yield kind

      override def unbind(key: String, value: LogStreamKind): String = stringBinder.unbind(key, value.entryName)
    }

  /**
    * Bind information units to route paths.
    */
  implicit def informationPathBindable(implicit stringBinder: PathBindable[String]): PathBindable[Information] =
    new PathBindable[Information] {
      override def bind(key: String, value: String): Either[String, Information] =
        for {
          boundValue <- stringBinder.bind(key, value)
          information <- Either
            .fromTry(Information(boundValue))
            .leftMap(_.getMessage)
        } yield information

      override def unbind(key: String, value: Information): String = stringBinder.unbind(key, value.toString)
    }
}

object PathBinders extends PathBinders
