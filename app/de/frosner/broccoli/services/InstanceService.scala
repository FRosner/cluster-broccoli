package de.frosner.broccoli.services

import javax.inject.{Singleton, Inject}

import de.frosner.broccoli.models.Instance
import play.api.Configuration
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class InstanceService @Inject() (configuration: Configuration, ws: WSClient, templateService: TemplateService) {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  def instances: Future[Seq[Instance]] = {
    val jobsRequest = ws.url(nomadBaseUrl + "/v1/jobs").withQueryString("prefix" -> nomadJobPrefix)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, names) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Name").map(_.as[JsString].value))
      ids.zip(names).flatMap{
        case (id, name) => templateService.template(name).map(
          template => Instance(id, template, Map("id" -> id))
        )
      }
    })
    jobsWithTemplate
  }

  def instance(id: String): Future[Option[Instance]] = instances.map(_.find(_.id == id))

}
