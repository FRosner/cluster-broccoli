package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit

import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.conf
import de.frosner.broccoli.nomad.models.{Job, Service, Task, TaskGroup}
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server
import Job.jobFormat
import de.frosner.broccoli.nomad.NomadConfiguration

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class NomadServiceSpec extends Specification with ServiceMocks {

  sequential

  "Requesting services for specific job" should {

    "ask consul for the services that nomad returns" in {
      val service1 = Service("my-service")
      val task1 = Task(Some(Seq(service1)))
      val taskGroup1 = TaskGroup(Some(Seq(task1)))
      val job = Job(Some(Seq(taskGroup1)))
      val consulService = mock(classOf[ConsulService])
      Server.withRouter() {
        case GET(p"/v1/job/my-job") =>
          Action {
            Results.Ok(Json.toJson(job))
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port")
          val nomadService = new NomadService(configuration, consulService, client)
          val jobId = "my-job"
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
          verify(consulService).requestServiceStatus(jobId, Iterable(service1.name))
          result === Seq(service1.name)
        }
      }
    }

    "not explode when receiving tasks without services" in {
      val service1 = Service("my-service")
      val task1 = Task(Some(Seq(service1)))
      val task2 = Task(None)
      val taskGroup1 = TaskGroup(Some(Seq(task1)))
      val taskGroup2 = TaskGroup(Some(Seq(task2)))
      val job = Job(Some(Seq(taskGroup1, taskGroup2)))
      val consulService = mock(classOf[ConsulService])
      Server.withRouter() {
        case GET(p"/v1/job/my-job") =>
          Action {
            Results.Ok(Json.toJson(job))
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port")
          val nomadService = new NomadService(configuration, consulService, client)
          val jobId = "my-job"
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
          verify(consulService).requestServiceStatus(jobId, Iterable(service1.name))
          result === Seq(service1.name)
        }
      }
    }

  }

}
