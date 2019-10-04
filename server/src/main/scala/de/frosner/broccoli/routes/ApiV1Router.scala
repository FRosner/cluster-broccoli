package de.frosner.broccoli.routes

import javax.inject.Inject

import com.google.inject.Provider
import de.frosner.broccoli.controllers._
import play.api.mvc.{Action, Results}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes for Broccoli REST API.
  *
  * @param templates Controller for templates
  * @param instances Controller for instances
  * @param about Controller for application information
  * @param status Controller for status application
  * @param security Controller for authentication
  */
class ApiV1Router @Inject()(
    templates: Provider[TemplateController],
    instances: Provider[InstanceController],
    about: Provider[AboutController],
    status: Provider[StatusController],
    security: Provider[SecurityController]
) extends SimpleRouter {
  override def routes: Routes = {
    // Templates
    case GET(p"/templates")         => templates.get.list
    case GET(p"/templates/$id")     => templates.get.show(id)
    case POST(p"templates/refresh") => templates.get.refresh
    // Instances
    case GET(p"/instances" ? q_o"templateId=$id") => instances.get.list(id)
    case GET(p"/instances/$id")                   => instances.get.show(id)
    case POST(p"/instances")                      => instances.get.create
    case POST(p"/instances/$id")                  => instances.get.update(id)
    case DELETE(p"/instances/$id")                => instances.get.delete(id)
    case GET(p"/instances/$id/tasks")             => instances.get.tasks(id)
    // About & status
    case GET(p"/about")  => about.get.about
    case GET(p"/status") => status.get.status
    // Authentication
    case POST(p"/auth/login")  => security.get.login
    case POST(p"/auth/logout") => security.get.logout
    case GET(p"/auth/verify")  => security.get.verify
    // Do not fall back to other routes for API requests, but return 404 directly
    case _ => Action(Results.NotFound)
  }
}
