package de.frosner.broccoli.routes

import javax.inject.Inject

import de.frosner.broccoli.controllers.InstanceController
import de.frosner.broccoli.nomad.models.LogStreamKind
import play.api.mvc.{Action, Results}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.http.ContentTypes._

class DownloadsRouter @Inject()(instances: InstanceController) extends SimpleRouter {
  private val logKind: PathBindableExtractor[LogStreamKind] = new PathBindableExtractor[LogStreamKind]

  override def routes: Routes = {
    case GET(p"/instances/$instanceId/allocations/$allocationId/tasks/$taskName/logs/${logKind(kind)}") =>
      instances.logFile(instanceId, allocationId, taskName, kind)
    case _ => Action(Results.NotFound(<h1>Download not found</h1>).as(HTML))
  }
}
