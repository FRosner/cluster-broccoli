package de.frosner.broccoli.nomad

import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.models.{InstanceStatus, Instance}
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import NomadActor._

class NomadActor @Inject() (configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  def receive = {
    case ListJobs =>
      val queryUrl = nomadBaseUrl + "/v1/jobs"
      val jobsRequest = ws.url(queryUrl).withQueryString("prefix" -> nomadJobPrefix)
      Logger.info(s"Requesting ${jobsRequest.uri}")
      val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
      val jobsWithTemplate = jobsResponse.map(jsArray => {
        val (ids, names) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Name").map(_.as[JsString].value))
        ids
      })
      jobsWithTemplate.onSuccess{ case ids => Logger.info(ids.mkString(", ")) }
//      sender() ! ""
  }
}

object NomadActor {
  def props(nomadBaseUrl: String, ws: WSClient) = Props(classOf[NomadActor], nomadBaseUrl, ws)

  case object ListJobs
}

