package de.frosner.broccoli.nomad.models

/**
  * Errors from the Nomad API.
  */
sealed trait NomadError

object NomadError {

  /**
    * A Nomad object (job, allocation, etc.) was not found
    */
  final case object NotFound extends NomadError

}
