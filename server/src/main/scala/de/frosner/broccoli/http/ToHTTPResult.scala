package de.frosner.broccoli.http

import cats.syntax.either._
import play.api.mvc.Result
import simulacrum._

import scala.language.implicitConversions

/**
  * Type class to convert a value to an HTTP result.
  *
  * @tparam T The type to convert to an HTTP result.
  */
@typeclass trait ToHTTPResult[T] {

  /**
    * Convert a value to an HTTP result.
    *
    * @param value The value to convert
    * @return The result
    */
  def toHTTPResult(value: T): Result
}

/**
  * ToHTTPResult instances for common types.
  */
trait ToHTTPResultInstances {

  /**
    * Convert an Either value to an HTTP result, provided that both sides have ToHttpResult instances.
    *
    * @param httpL Instance for the left type
    * @param httpR Instance for the right right
    * @tparam L Type of the left side
    * @tparam R Type of the right side
    * @return A ToHTTPResult instance for an Either of L and R
    */
  implicit def eitherToHttpResult[L, R](implicit httpL: ToHTTPResult[L],
                                        httpR: ToHTTPResult[R]): ToHTTPResult[Either[L, R]] =
    ToHTTPResult.instance { value: Either[L, R] =>
      value.fold(httpL.toHTTPResult, httpR.toHTTPResult)
    }
}

object ToHTTPResult extends ToHTTPResultInstances {

  /**
    * Conveniently define a ToHTTPResult instance.
    *
    * @param f A function to convert a value to an HTTP result
    * @tparam T The type to convert to an HTTP result
    * @return An instance using the given function to convert values of T to HTTP results
    */
  def instance[T](f: T => Result): ToHTTPResult[T] = new ToHTTPResult[T] {
    override def toHTTPResult(value: T): Result = f(value)
  }
}
