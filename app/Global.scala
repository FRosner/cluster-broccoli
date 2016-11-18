import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.HeaderNames._

object Global extends GlobalSettings {

  override def doFilter(action: EssentialAction): EssentialAction = EssentialAction { request =>
    action.apply(request).map(_.withHeaders(
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
    ))
  }

}