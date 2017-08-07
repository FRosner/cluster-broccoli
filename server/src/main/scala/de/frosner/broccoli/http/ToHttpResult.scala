package de.frosner.broccoli.http

import play.api.mvc.Result
import simulacrum._

import scala.language.implicitConversions

/**
  * Type class to convert a value to an HTTP result.
  *
  * @tparam T The type to convert to an HTTP result.
  */
@typeclass trait ToHttpResult[T] {

  /**
    * Convert a value to an HTTP result.
    *
    * @param value The value to convert
    * @return The result
    */
  def toHttpResult(value: T): Result
}

object ToHttpResult {

  /**
    * Conveniently define a ToHttpResult instance.
    *
    * @param f A function to convert a value to an HTTP result
    * @tparam T The type to convert to an HTTP result
    * @return An instance using the given function to convert values of T to HTTP results
    */
  def instance[T](f: T => Result): ToHttpResult[T] = new ToHttpResult[T] {
    override def toHttpResult(value: T): Result = f(value)
  }
}
