package de.frosner.broccoli.routes

import javax.inject.{Inject, Provider}

import de.frosner.broccoli.controllers.InstanceController
import de.frosner.broccoli.routes.Extractors._
import play.api.mvc.{Action, Results}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.http.ContentTypes._

class DownloadsRouter @Inject()(instances: Provider[InstanceController]) extends SimpleRouter {

  override def routes: Routes = {
    case GET(
        p"/instances/$instanceId/allocations/$allocationId/tasks/$taskName/logs/${logKind(kind)}" ? q_o"offset=${information(offset)}") =>
      instances.get.logFile(instanceId, allocationId, taskName, kind, offset)
    case _ => Action(Results.NotFound(<h1>Download not found</h1>).as(HTML))
  }
}
