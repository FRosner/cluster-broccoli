package de.frosner.broccoli

import javax.inject.Inject

import de.frosner.broccoli.controllers._
import play.api.mvc.{Action, Results}
import play.api.routing.Router.Routes
import play.api.routing.sird._
import play.api.routing.SimpleRouter

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
    templates: TemplateController,
    instances: InstanceController,
    about: AboutController,
    status: StatusController,
    security: SecurityController
) extends SimpleRouter {
  override def routes: Routes = {
    // Templates
    case GET(p"/templates")     => templates.list
    case GET(p"/templates/$id") => templates.show(id)
    // Instances
    case GET(p"/instances" ? q_o"templateId=$id") => instances.list(id)
    case GET(p"/instances/$id")                   => instances.show(id)
    case POST(p"/instances")                      => instances.create
    case POST(p"/instances/$id")                  => instances.update(id)
    case DELETE(p"/instances/$id")                => instances.delete(id)
    case GET(p"/instances/$id/tasks")             => instances.tasks(id)
    // About & status
    case GET(p"/about")  => about.about
    case GET(p"/status") => status.status
    // Authentication
    case POST(p"/auth/login")  => security.login
    case POST(p"/auth/logout") => security.logout
    case GET(p"/auth/verify")  => security.verify
    // Do not fall back to other routes for API requests, but return 404 directly
    case _ => Action(Results.NotFound)
  }
}
