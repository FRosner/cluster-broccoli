package de.frosner.broccoli

import simulacrum._

import scala.language.implicitConversions

/**
  * Typeclass to remove secrets from values of a type.
  *
  * @tparam T The type containing secrets
  */
@typeclass trait RemoveSecrets[T] {

  /**
    * Remove secrets from a value.
    *
    * @param value The value to remove secrets from
    * @return The value without secrets
    */
  def removeSecrets(value: T): T
}

object RemoveSecrets {

  /**
    * Create an instance of RemoveSecrets.
    *
    * @param remove The function to remove secrets from a value
    */
  def instance[T](remove: T => T): RemoveSecrets[T] = new RemoveSecrets[T] {
    override def removeSecrets(value: T): T = remove(value)
  }
}
