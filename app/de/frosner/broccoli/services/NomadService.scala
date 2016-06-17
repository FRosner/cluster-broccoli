package de.frosner.broccoli.services

import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.models.{InstanceStatus, Instance}
import de.frosner.broccoli.services.InstanceService.NomadStatuses
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json.{JsArray, JsString}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

class NomadService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  def receive = {
    case GetStatuses =>
      val sendingService = sender()
      val queryUrl = nomadBaseUrl + "/v1/jobs"
      val jobsRequest = ws.url(queryUrl).withQueryString("prefix" -> nomadJobPrefix)
      Logger.info(s"Requesting ${jobsRequest.uri}")
      val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
      val jobsWithTemplate = jobsResponse.map(jsArray => {
        val (ids, statuses) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Status").map(_.as[JsString].value))
        (ids, statuses)
      })
      jobsWithTemplate.onSuccess{ case (ids, statuses) =>
        Logger.info(s"Received a status update of jobs: ${ids.mkString(", ")}")
        val idsAndStatuses = ids.zip(statuses.map{
          case "running" => InstanceStatus.Running
          case default => Logger.warn(s"Unmatched status received: $default")
            InstanceStatus.Unknown
        })
        sendingService ! NomadStatuses(idsAndStatuses.toMap)
      }
  }
}

object NomadService {
  case object GetStatuses
}

