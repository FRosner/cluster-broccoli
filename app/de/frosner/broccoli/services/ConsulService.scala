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

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class ConsulService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val consulBaseUrl = configuration.getString(conf.CONSUL_URL_KEY).getOrElse(conf.CONSUL_URL_DEFAULT)
  private val consulDomain: Option[String] = configuration.getString(conf.CONSUL_DOMAIN_KEY)

  def receive = {
    case ServiceStatusRequest(jobId, serviceNames) =>
      val sendingService = sender()
      val serviceResponses: Iterable[Future[Seq[Service]]] = serviceNames.map { name =>
        val queryUrl = consulBaseUrl + s"/v1/catalog/service/$name"
        val request = ws.url(queryUrl)
        Logger.info(s"Requesting service information (${request.uri})")
        request.get().map { response =>
          val responseJsonArray = response.json.as[JsArray]
          responseJsonArray.value.map { serviceJson =>
            val fields = serviceJson.as[JsObject].value
            // TODO proper JSON parsing with exception handling
            val serviceName = fields("ServiceName").as[JsString].value
            val serviceProtocol = ConsulService.extractProtocolFromTags(fields("ServiceTags")) match {
              case Some(protocol) => protocol
              case None => Logger.warn("Service did not specify a single protocol tag (e.g. protocol-https). Assuming https.")
                "https"
            }
            val serviceAddress = consulDomain.map { domain =>
              fields("ServiceName").as[JsString].value + ".service." + domain
            }.getOrElse {
              fields("ServiceAddress").as[JsString].value
            }
            val servicePort = fields("ServicePort").as[JsNumber].value.toInt
            Service(
              name = serviceName,
              protocol = serviceProtocol,
              address = serviceAddress,
              port = servicePort
            )
          }
        }

      }
      val serviceResponse = Future.sequence(serviceResponses)
      serviceResponse.onComplete {
        case Success(services: Iterable[Seq[Service]]) => sendingService ! ConsulServices(jobId, services.flatten)
        case Failure(throwable) =>
          Logger.error(throwable.toString)
          sendingService ! ConsulNotReachable
      }
  }

}

object ConsulService {
  case class ServiceStatusRequest(jobId: String, serviceNames: Iterable[String])

  def extractProtocolFromTags(tagsJs: JsValue): Option[String] = {
    val tags = tagsJs.as[JsArray].value.map(_.as[JsString].value)
    val protocolTags = tags.filter(_.startsWith("protocol-"))
    val protocols = protocolTags.map(_.split('-')(1))
    protocols.size match {
      case 1 => Some(protocols.head)
      case others => None
    }
  }
}

