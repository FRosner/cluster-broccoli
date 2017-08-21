package de.frosner.broccoli.routes

import de.frosner.broccoli.nomad.models.LogStreamKind
import play.api.routing.sird.PathBindableExtractor
import squants.information.Information

/**
  * Additional extractors for string DSL routes.
  */
trait Extractors {
  import PathBinders._

  /**
    * The kind of a log.
    */
  val logKind: PathBindableExtractor[LogStreamKind] = new PathBindableExtractor[LogStreamKind]

  /**
    * Extract units of information from paths.
    */
  val information: PathBindableExtractor[Information] = new PathBindableExtractor[Information]
}

object Extractors extends Extractors
