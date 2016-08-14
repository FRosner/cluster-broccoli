package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor._
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus, Service, ServiceStatus}
import de.frosner.broccoli.services.ConsulService.ServiceStatusRequest
import de.frosner.broccoli.services.InstanceService.{ConsulServices, NomadNotReachable, NomadStatuses}
import de.frosner.broccoli.services.NomadService._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


class ConsulService @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val consulBaseUrl = configuration.getString(conf.CONSUL_URL_KEY).getOrElse(conf.CONSUL_URL_DEFAULT)
  private val consulDomain: Option[String] = {
    val lookupMethod = configuration.getString(conf.CONSUL_LOOKUP_METHOD_KEY).getOrElse(conf.CONSUL_LOOKUP_METHOD_IP)
    lookupMethod match {
      case conf.CONSUL_LOOKUP_METHOD_DNS => {
        val requestUrl = consulBaseUrl + "/v1/agent/self"
        Logger.info(s"Requesting Consul domain from $requestUrl")
        val request = ws.url(requestUrl)
        val response = request.get().map(_.json.as[JsObject])
        val eventuallyConsulDomain = response.map{ jsObject =>
          (jsObject \ "Config" \ "Domain").get.as[JsString].value.stripSuffix(".")
        }
        val tryConsulDomain = Try(Await.result(eventuallyConsulDomain, Duration(5, TimeUnit.SECONDS))).recoverWith{
          case throwable =>
            Logger.warn(s"Cannot reach Consul to read the configuration from $requestUrl, falling back to IP based lookup: $throwable")
            Failure(throwable)
        }
        tryConsulDomain.foreach(domain => Logger.info(s"Advertising Consul entities through DNS using '$domain' as the domain."))
        tryConsulDomain.toOption
      }
      case conf.CONSUL_LOOKUP_METHOD_IP =>
        Logger.info("Advertising Consul entities through IP addresses.")
        None
      case default =>
        throw new IllegalArgumentException(s"Configuration property ${conf.CONSUL_LOOKUP_METHOD_KEY} = $default is invalid.")
    }
  }

  def receive = {
    case ServiceStatusRequest(jobId, serviceNames) =>
      val sendingService = sender()
      val serviceResponses: Iterable[Future[Seq[Service]]] = serviceNames.map { name =>
        val catalogQueryUrl = consulBaseUrl + s"/v1/catalog/service/$name"
        val catalogRequest = ws.url(catalogQueryUrl)
        Logger.debug(s"Requesting service information (${catalogRequest.uri})")
        val healthQueryUrl = consulBaseUrl + s"/v1/health/service/$name?passing"
        val healthRequest = ws.url(healthQueryUrl)
        Logger.debug(s"Requesting service health (${healthRequest.uri})")
        val requests = Future.sequence(List(catalogRequest.get(), healthRequest.get()))
        requests.map { case List(catalogResponse, healthResponse) =>
          val responseJsonArray = catalogResponse.json.as[JsArray]
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
            val serviceStatus = {
              val healthyServiceInstances = healthResponse.json.as[JsArray]
              if (healthyServiceInstances.value.isEmpty) ServiceStatus.Failing else ServiceStatus.Passing
            }
            Service(
              name = serviceName,
              protocol = serviceProtocol,
              address = serviceAddress,
              port = servicePort,
              status = serviceStatus
            )
          }
        }
      }
      val serviceResponse = Future.sequence(serviceResponses)
      serviceResponse.onComplete {
        case Success(services: Iterable[Seq[Service]]) => sendingService ! ConsulServices(jobId, services.flatten)
        case Failure(throwable) =>
          Logger.error(throwable.toString)
          val unknownServices = serviceNames.map(
            name => Service(
              name = name,
              protocol = "",
              address = "",
              port = 80,
              status = ServiceStatus.Unknown
            )
          )
          sendingService ! ConsulServices(jobId, unknownServices)
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

