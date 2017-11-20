package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.conf
import de.frosner.broccoli.logging.logExecutionTime
import de.frosner.broccoli.models.{Service, ServiceStatus}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton()
class ConsulService @Inject()(configuration: Configuration, ws: WSClient)(implicit ec: ExecutionContext) {

  private val log = Logger(getClass)

  log.info(s"Starting $this")

  private lazy val consulBaseUrl = configuration.getString(conf.CONSUL_URL_KEY).getOrElse(conf.CONSUL_URL_DEFAULT)
  private lazy val consulDomain: Option[String] = {
    val lookupMethod = configuration.getString(conf.CONSUL_LOOKUP_METHOD_KEY).getOrElse(conf.CONSUL_LOOKUP_METHOD_IP)
    lookupMethod match {
      case conf.CONSUL_LOOKUP_METHOD_DNS => {
        val requestUrl = consulBaseUrl + "/v1/agent/self"
        logExecutionTime(s"GET $requestUrl") {
          val request = ws.url(requestUrl)
          val response = request.get().map(_.json.as[JsObject])
          val eventuallyConsulDomain = response.map { jsObject =>
            (jsObject \ "Config" \ "Domain").get.as[JsString].value.stripSuffix(".")
          }
          val tryConsulDomain = Try(Await.result(eventuallyConsulDomain, Duration(5, TimeUnit.SECONDS))).recoverWith {
            case throwable =>
              log.warn(
                s"Cannot reach Consul to read the configuration from $requestUrl, falling back to IP based lookup: $throwable")
              Failure(throwable)
          }
          tryConsulDomain.foreach(domain =>
            log.info(s"Advertising Consul entities through DNS using '$domain' as the domain."))
          tryConsulDomain.toOption
        }(log.info(_))
      }
      case conf.CONSUL_LOOKUP_METHOD_IP =>
        log.info("Advertising Consul entities through IP addresses.")
        None
      case default =>
        throw new IllegalArgumentException(
          s"Configuration property ${conf.CONSUL_LOOKUP_METHOD_KEY} = $default is invalid.")
    }
  }

  def requestServicesStatuses(jobs: Map[String, Seq[String]]): Future[Map[String, Seq[Service]]] =
    Future
      .sequence(jobs.map {
        case (jobId, services) =>
          requestServiceStatus(jobId, services)
      })
      .map(_.toMap)

  def requestServiceStatus(jobId: String, serviceNames: Seq[String]): Future[(String, Seq[Service])] = {
    val serviceResponses = serviceNames.map { name =>
      val catalogQueryUrl = consulBaseUrl + s"/v1/catalog/service/$name"
      val catalogRequest = ws.url(catalogQueryUrl)
      val healthQueryUrl = consulBaseUrl + s"/v1/health/service/$name?passing"
      val healthRequest = ws.url(healthQueryUrl)
      val requests = Future.sequence(List(catalogRequest.get(), healthRequest.get()))
      requests.map {
        case List(catalogResponse, healthResponse) =>
          val catalogResponseJson = catalogResponse.json
          val healthResponseJson = healthResponse.json
          log.debug(s"${catalogRequest.uri} => $catalogResponseJson")
          log.debug(s"${healthRequest.uri} => $healthResponseJson")
          val responseJsonArray = catalogResponseJson.as[JsArray]
          responseJsonArray.value.map { serviceJson =>
            val fields = serviceJson.as[JsObject].value
            // TODO proper JSON parsing with exception handling
            val serviceName = fields("ServiceName").as[JsString].value
            val serviceProtocol = ConsulService.extractProtocolFromTags(fields("ServiceTags")) match {
              case Some(protocol) => protocol
              case None =>
                log.warn("Service did not specify a single protocol tag (e.g. protocol-https). Assuming https.")
                "https"
            }
            val serviceAddress = consulDomain
              .map { domain =>
                fields("ServiceName").as[JsString].value + ".service." + domain
              }
              .getOrElse {
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
    val serviceResponse = Future.sequence(serviceResponses)
    def unknownService(name: String) = Service(
      name = name,
      protocol = "",
      address = "",
      port = 80,
      status = ServiceStatus.Unknown
    )
    val serviceResponseDone = serviceResponse.map { services =>
      {
        val healthyOrUnhealthyServices = services.flatten.map(service => (service.name, service)).toMap
        val allServices = serviceNames.map { name =>
          healthyOrUnhealthyServices.getOrElse(name, unknownService(name))
        }
        (jobId, allServices)
      }
    }
    serviceResponseDone.onFailure {
      case throwable =>
        log.error(s"Failed to get service statuses for job '$jobId' from Consul: ${throwable.toString}")
    }
    serviceResponseDone
  }

}

object ConsulService {

  def extractProtocolFromTags(tagsJs: JsValue): Option[String] = {
    val tags = tagsJs.as[JsArray].value.map(_.as[JsString].value)
    val protocolTags = tags.filter(_.startsWith("protocol-"))
    val protocols = protocolTags.map(_.split('-')(1))
    protocols.size match {
      case 1      => Some(protocols.head)
      case others => None
    }
  }

}
