package de.frosner.broccoli.http

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Add Broccoli access control headers to responses.
  *
  * Add the following headers to responses:
  *
  * - Access-Control-Allow-Credentials: true to expose responses to the webpage
  *
  * @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials
  */
class AccessControlFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  /**
    * Add access control headers to the response.
    *
    * @param next The next filter, to turn a header into a result
    * @param request The incoming request
    * @return The result of the request with access control headers added.
    */
  override def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] =
    next(request).map(_.withHeaders(ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"))
}
