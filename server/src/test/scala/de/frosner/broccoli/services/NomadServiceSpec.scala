package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.nomad.models.Job.jobFormat
import de.frosner.broccoli.nomad.models._
import de.frosner.broccoli.nomad.models.NodeResources._
import org.mockito.Mockito._
import org.specs2.mutable.Specification
import play.api.{BuiltInComponents, Mode}
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.api.test._
import play.core.server.{NettyServerComponents, Server, ServerConfig}
import squants.information.InformationConversions._
import squants.time.FrequencyConversions._

import scala.concurrent.ExecutionContext.Implicits.global
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
      val jobId = "my-job"
      Server.withRouter() {
        case GET(p"/v1/job/my-job") =>
          Action {
            Results.Ok(Json.toJson(job))
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port")
          val nomadService = new NomadService(configuration, client)
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
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
      val jobId = "my-job"
      Server.withRouter() {
        case GET(p"/v1/job/my-job") =>
          Action {
            Results.Ok(Json.toJson(job))
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port")
          val nomadService = new NomadService(configuration, client)
          val result = Await.result(nomadService.requestServices(jobId), Duration(5, TimeUnit.SECONDS))
          result === Seq(service1.name)
        }
      }
    }

  }

  "Getting node resources for user" should {
    "work as expected" in {
      val port = 1024 + scala.util.Random.nextInt(64511)
      val allocDirInfo =
        AllocDirInfo(61577662464L, "", 25.128018856048584, "", 135148244992L, 66681864192L, 49.339792903671956)
      val cpuInfos =
        Seq(
          CPUInfo("cpu0", 95.04950470034078, 2.9702970276487837, 4.950495046081306, 0.9900990092162613),
          CPUInfo("cpu1", 97.93814437344903, 1.0309278372786437, 2.0618556745572874, 1.0309278372786437)
        )
      val diskInfos =
        Seq(
          DiskInfo(61577662464L, "/dev/vda1", 25.128018856048584, "/", 135148244992L, 66681864192L, 49.339792903671956),
          DiskInfo(
            61577662464L,
            "/dev/vda1",
            25.128018856048584,
            "/var/lib/docker/aufs",
            135148244992L,
            66681864192L,
            49.339792903671956
          )
        )
      val memoryInfo = MemoryInfo(15642181632L, 1030324224, 16824614912L, 1182433280)
      val resourceInfo =
        ResourceInfo(allocDirInfo, cpuInfos, 161.0736960530685, diskInfos, memoryInfo, 1535023884681657859L, 18909052)
      val nodesResources =
        Seq(
          NodeResources("b9747124-1854-64a5-522b-1f1f32747eda", "nooe-01", resourceInfo)
        )

      // test server for node running nomad
      val server =
        new NettyServerComponents with BuiltInComponents {
          lazy val router = Router.from {
            case GET(p"/v1/client/stats") => Action { Results.Ok(Json.toJson(resourceInfo)) }
          }
          override lazy val serverConfig = ServerConfig(
            port = Some(port),
            address = "127.0.0.1",
            mode = Mode.Test
          )
          override lazy val actorSystem: ActorSystem = ActorSystem()
        }.server

      Server.withRouter() {
        case GET(p"/v1/nodes") =>
          Action {
            Results.Ok(Json.parse("""
                |[
                |  {
                |    "ID": "b9747124-1854-64a5-522b-1f1f32747eda",
                |    "Datacenter": "dc1",
                |    "Name": "nooe-01",
                |    "NodeClass": "",
                |    "Drain": false,
                |    "Status": "ready",
                |    "StatusDescription": "",
                |    "CreateIndex": 2299184,
                |    "ModifyIndex": 11099317
                |  },
                |  {
                |    "ID": "b9747124-1854-64a5-522b-1f1f32747edb",
                |    "Datacenter": "dc1",
                |    "Name": "azfr-02",
                |    "NodeClass": "",
                |    "Drain": false,
                |    "Status": "ready",
                |    "StatusDescription": "",
                |    "CreateIndex": 2299184,
                |    "ModifyIndex": 11099317
                |  }
                |]
              """.stripMargin))
          }
        case GET(p"/v1/node/b9747124-1854-64a5-522b-1f1f32747eda") =>
          Action {
            Results.Ok(Json.parse(s"""
                |{
                |  "ID": "b9747124-1854-64a5-522b-1f1f32747eda",
                |  "SecretID": "",
                |  "Datacenter": "dc1",
                |  "Name": "nooe-01",
                |  "HTTPAddr": "127.0.0.1:$port",
                |  "TLSEnabled": false,
                |  "Attributes": {
                |    "unique.storage.volume": "/dev/vda1",
                |    "cpu.modelname": "QEMU Virtual CPU version (cpu64-rhel6)",
                |    "kernel.name": "linux",
                |    "unique.consul.name": "nooe-01",
                |    "cpu.totalcompute": "4594",
                |    "os.signals": "SIGURG,SIGINT,SIGKILL,SIGTSTP,SIGUSR1,SIGXCPU,SIGILL,SIGIOT,SIGTTIN,SIGCHLD,SIGTRAP,SIGABRT,SIGBUS,SIGPROF,SIGQUIT,SIGSEGV,SIGCONT,SIGSYS,SIGTERM,SIGTTOU,SIGUSR2,SIGXFSZ,SIGSTOP,SIGALRM,SIGFPE,SIGHUP,SIGIO,SIGPIPE",
                |    "unique.hostname": "nooe-01",
                |    "consul.server": "false",
                |    "os.name": "ubuntu",
                |    "unique.storage.bytestotal": "135148244992",
                |    "consul.version": "0.7.5",
                |    "nomad.revision": "da97ad4dddff52d7d8d2a2cc81aa73b64558cf50",
                |    "nomad.version": "0.5.4",
                |    "driver.docker": "1",
                |    "consul.datacenter": "dc1",
                |    "driver.docker.volumes.enabled": "1",
                |    "cpu.numcores": "2",
                |    "kernel.version": "4.4.0-109-generic",
                |    "cpu.frequency": "2297",
                |    "driver.docker.version": "17.05.0-ce",
                |    "memory.totalbytes": "16824614912",
                |    "unique.storage.bytesfree": "54166417408",
                |    "os.version": "16.04",
                |    "consul.revision": "'21f2d5a'",
                |    "unique.cgroup.mountpoint": "/sys/fs/cgroup",
                |    "unique.network.ip-address": "10.250.24.141",
                |    "cpu.arch": "amd64"
                |  },
                |  "Resources": {
                |    "CPU": 4594,
                |    "MemoryMB": 16045,
                |    "DiskMB": 51657,
                |    "IOPS": 0,
                |    "Networks": [
                |      {
                |        "Device": "eth0",
                |        "CIDR": "10.250.24.141/32",
                |        "IP": "10.250.24.141",
                |        "MBits": 1024,
                |        "ReservedPorts": null,
                |        "DynamicPorts": null
                |      }
                |    ]
                |  },
                |  "Reserved": {
                |    "CPU": 0,
                |    "MemoryMB": 0,
                |    "DiskMB": 0,
                |    "IOPS": 0,
                |    "Networks": null
                |  },
                |  "Links": {
                |    "consul": "dc1.nooe-01"
                |  },
                |  "Meta": {
                |    "oe_tag": "nooe",
                |    "deployment_env_tag": "default"
                |  },
                |  "NodeClass": "",
                |  "ComputedClass": "v1:11004109724528736728",
                |  "Drain": false,
                |  "Status": "ready",
                |  "StatusDescription": "",
                |  "StatusUpdatedAt": 1535643851,
                |  "CreateIndex": 2299184,
                |  "ModifyIndex": 11099317
                |}
              """.stripMargin))
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port")
          val nomadService = new NomadService(configuration, client)
          val result =
            Await.result(nomadService.getNodeResources(new Account("nooe-admin", "nooe-*", Role.Administrator)),
                         Duration(5, TimeUnit.SECONDS))
          server.stop() // stop test server
          result === nodesResources
        }
      }
    }
  }
}
