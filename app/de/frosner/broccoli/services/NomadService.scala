package de.frosner.broccoli.services

import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json.{JsArray, JsString}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

class NomadService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

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

object NomadService {
  case object ListJobs
}

