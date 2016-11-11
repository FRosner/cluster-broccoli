package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Singleton, Inject}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus, Service, ServiceStatus}
import de.frosner.broccoli.services.InstanceService.{ConsulServices, NomadNotReachable, NomadStatuses}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


@Singleton()
class ConsulService @Inject()(configuration: Configuration,
                              ws: WSClient) {

  implicit val defaultContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  @volatile
  var serviceStatuses: Map[String, Map[String, Service]] = Map.empty

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

  def getServiceStatusesOrDefault(id: String): Map[String, Service] = {
    serviceStatuses.getOrElse(id, Map.empty)
  }

  def requestServiceStatus(jobId: String, serviceNames: Iterable[String]) = {
    val serviceResponses: Iterable[Future[Seq[Service]]] = serviceNames.map { name =>
      val catalogQueryUrl = consulBaseUrl + s"/v1/catalog/service/$name"
      val catalogRequest = ws.url(catalogQueryUrl)
      val healthQueryUrl = consulBaseUrl + s"/v1/health/service/$name?passing"
      val healthRequest = ws.url(healthQueryUrl)
      val requests = Future.sequence(List(catalogRequest.get(), healthRequest.get()))
      requests.map { case List(catalogResponse, healthResponse) =>
        Logger.info(s"${catalogRequest.uri} ${healthRequest.uri}")
        val catalogResponseJson = catalogResponse.json
        val healthResponseJson = healthResponse.json
        Logger.debug(s"${catalogRequest.uri} => $catalogResponseJson")
        Logger.debug(s"${healthRequest.uri} => $healthResponseJson")
        val responseJsonArray = catalogResponseJson.as[JsArray]
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
            val healthyServiceInstances = healthResponseJson.as[JsArray]
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
    val serviceResponse = Try(Await.result(Future.sequence(serviceResponses), Duration(5, TimeUnit.SECONDS)))
    def unknownService(name: String) = Service(
      name = name,
      protocol = "",
      address = "",
      port = 80,
      status = ServiceStatus.Unknown
    )
    serviceResponse match {
      case Success(services: Iterable[Seq[Service]]) => {
        val healthyOrUnhealthyServices = services.flatten.map(service => (service.name, service)).toMap
        val allServices = serviceNames.map { name =>
          (name, healthyOrUnhealthyServices.getOrElse(name, unknownService(name)))
        }.toMap
        serviceStatuses = serviceStatuses.updated(jobId, allServices)
      }
      case Failure(throwable) =>
        Logger.error(throwable.toString)
        val unknownServices = serviceNames.map(unknownService)
        ConsulServices(jobId, unknownServices)
        serviceStatuses = serviceStatuses.updated(jobId, unknownServices.map(service => (service.name, service)).toMap)
    }
  }

}

object ConsulService {

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

