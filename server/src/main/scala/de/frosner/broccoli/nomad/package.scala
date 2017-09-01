package de.frosner.broccoli

import cats.data.EitherT
import de.frosner.broccoli.nomad.models.NomadError

import scala.concurrent.Future

package object nomad {

  /**
    * Monad for Nomad actions.
    *
    * @tparam R The result type.
    */
  type NomadT[R] = EitherT[Future, NomadError, R]

}
