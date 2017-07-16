package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit

import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.conf
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class NomadServiceSpec extends Specification with ServiceMocks {

  sequential

  "Requesting services for specific job" should {

    "ask consul for the services that nomad returns" in {
      val service1Name = "my-service"
      val service1 = JsObject(Map(
        "Name" -> JsString(service1Name)
      ))
      val services = JsArray(Seq(service1))
      val task1 = JsObject(Map(
        "Services" -> services
      ))
      val tasks = JsArray(Seq(task1))
      val taskGroup1 = JsObject(Map(
        "Tasks" -> tasks
      ))
      val taskGroups = JsArray(Seq(taskGroup1))
      val job = JsObject(Map(
        "TaskGroups" -> taskGroups
      ))
      val consulService = mock(classOf[ConsulService])
      Server.withRouter() {
        case GET(p"/v1/job/my-job") => Action {
          Results.Ok(job)
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = Configuration.from(Map(
            conf.NOMAD_URL_KEY -> s"http://localhost:$port"
          ))
          val nomadService = new NomadService(configuration, consulService, client)
          val jobId = "my-job"
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
          verify(consulService).requestServiceStatus(jobId, Iterable(service1Name))
          result === Seq(service1Name)
        }
      }
    }

    "not explode when receiving tasks without services" in {
      val service1Name = "my-service"
      val service1 = JsObject(Map(
        "Name" -> JsString(service1Name)
      ))
      val services = JsArray(Seq(service1))
      val task1 = JsObject(Map(
        "Services" -> services
      ))
      val tasks = JsArray(Seq(task1))
      val taskGroup1 = JsObject(Map(
        "Tasks" -> tasks
      ))
      val services2 = JsNull
      val task2 = JsObject(Map(
        "Services" -> services2
      ))
      val tasks2 = JsArray(Seq(task2))
      val taskGroup2 = JsObject(Map(
        "Tasks" -> tasks2
      ))
      val taskGroups = JsArray(Seq(taskGroup1, taskGroup2))
      val job = JsObject(Map(
        "TaskGroups" -> taskGroups
      ))
      val consulService = mock(classOf[ConsulService])
      Server.withRouter() {
        case GET(p"/v1/job/my-job") => Action {
          Results.Ok(job)
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = Configuration.from(Map(
            conf.NOMAD_URL_KEY -> s"http://localhost:$port"
          ))
          val nomadService = new NomadService(configuration, consulService, client)
          val jobId = "my-job"
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
          verify(consulService).requestServiceStatus(jobId, Iterable(service1Name))
          result === Seq(service1Name)
        }
      }
    }

  }

}
