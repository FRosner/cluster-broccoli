package de.frosner.broccoli.services

import javax.inject.{Singleton, Inject}

import de.frosner.broccoli.models.Instance
import play.api.Configuration
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Singleton
class InstanceService @Inject() (configuration: Configuration, ws: WSClient, templateService: TemplateService) {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  def instanceList: Future[Seq[String]] = {
    val jobsRequest = ws.url(nomadBaseUrl + "/v1/jobs").withQueryString("prefix" -> nomadJobPrefix)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val names = jsArray \\ "Name"
      names.map(_.as[JsString].value).filter(templateService.isTemplate)
    })
    jobsWithTemplate
  }

}
