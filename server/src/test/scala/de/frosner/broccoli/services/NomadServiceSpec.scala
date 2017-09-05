package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit

import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.nomad.models.Job.jobFormat
import de.frosner.broccoli.nomad.models._
import org.mockito.Mockito._
import org.specs2.mutable.Specification
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server
import squants.information.InformationConversions._
import squants.time.FrequencyConversions._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class NomadServiceSpec extends Specification with ServiceMocks {

  sequential

  "Requesting services for specific job" should {

    "ask consul for the services that nomad returns" in {
      val service = Service("my-service")
      val resources = Resources(
        shapeless.tag[Resources.CPU](20.megahertz),
        shapeless.tag[Resources.Memory](1.gigabytes)
      )
      val task = Task(shapeless.tag[Task.Name]("foo"), resources, Some(Seq(service)))
      val taskGroup = TaskGroup(Seq(task))
      val job = Job(Seq(taskGroup))
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
          verify(consulService).requestServiceStatus(jobId, Seq(service.name))
          result === Seq(service.name)
        }
      }
    }

    "not explode when receiving tasks without services" in {
      val service1 = Service("my-service")
      val resources = Resources(
        shapeless.tag[Resources.CPU](100.megahertz),
        shapeless.tag[Resources.Memory](100.megabytes)
      )
      val task1 = Task(shapeless.tag[Task.Name]("foo1"), resources, Some(Seq(service1)))
      val task2 = Task(shapeless.tag[Task.Name]("foo2"), resources, None)
      val taskGroup1 = TaskGroup(Seq(task1))
      val taskGroup2 = TaskGroup(Seq(task2))
      val job = Job(Seq(taskGroup1, taskGroup2))
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
          verify(consulService).requestServiceStatus(jobId, Seq(service1.name))
          result === Seq(service1.name)
        }
      }
    }

  }

}
