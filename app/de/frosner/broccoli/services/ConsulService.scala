package de.frosner.broccoli.services

import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus, Service}
import de.frosner.broccoli.services.ConsulService.ServiceStatusRequest
import de.frosner.broccoli.services.InstanceService.{ConsulNotReachable, ConsulServices, NomadNotReachable, NomadStatuses}
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success}


// TODO allow asking for services
class ConsulService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val consulBaseUrl = configuration.getString(conf.CONSUL_URL_KEY).getOrElse(conf.CONSUL_URL_DEFAULT)

  def receive = {
    case ServiceStatusRequest(jobId, serviceNames) =>
      val sendingService = sender()
      serviceNames.foreach { name =>
        val queryUrl = consulBaseUrl + s"/v1/catalog/service/$name"
        val request = ws.url(queryUrl)
        Logger.info(s"Requesting service information (${request.uri})")
        request.get().onComplete {
          case Success(response) =>
            val responseJsonArray = response.json.as[JsArray]
            val responseServices = responseJsonArray.value.map { serviceJson =>
              val fields = serviceJson.as[JsObject].value
              // TODO proper JSON parsing with exception handling
              Service(
                name = fields("ServiceName").as[JsString].value,
                protocol = "http", // TODO read from tags
                address = fields("ServiceAddress").as[JsString].value,
                port = fields("ServicePort").as[JsNumber].value.toInt
              )
            }
            sendingService ! ConsulServices(jobId, responseServices)
          case Failure(throwable) =>
            Logger.error(throwable.toString)
            sendingService ! ConsulNotReachable
        }
      }
  }

}

object ConsulService {
  case class ServiceStatusRequest(jobId: String, serviceNames: Iterable[String])
}

