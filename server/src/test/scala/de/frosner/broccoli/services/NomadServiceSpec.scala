package de.frosner.broccoli.services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.nomad.NomadConfiguration
import de.frosner.broccoli.nomad.models.Job.jobFormat
import de.frosner.broccoli.nomad.models._
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
          val configuration = NomadConfiguration(url = s"http://localhost:$port", "NOMAD_BROCCOLI_TOKEN")
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
          val configuration = NomadConfiguration(url = s"http://localhost:$port", "NOMAD_BROCCOLI_TOKEN")
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
      val nodesResources =
        NodeResources(
          "b9747124-1854-64a5-522b-1f1f32747eda",
          "nooe-01",
          TotalResources(4594, 16045, 51657),
          HostResources(45, 1130258432L, 16824614912L, 67820965888L, 135148244992L),
          Map(
            "51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29" ->
              AllocatedResources(
                id = "51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29",
                name = "nooe-vault.nooe-vault[0]",
                cpu = 500,
                memoryMB = 1024,
                diskMB = 300
              )
          ),
          Map(
            "51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29" ->
              AllocatedResourcesUtilization(
                id = "51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29",
                name = "nooe-vault.nooe-vault[0]",
                cpu = 15,
                memory = 9945088
              )
          ),
          TotalResources(500, 1024, 300),
          TotalUtilization(15, 9945088)
        )

      // test server for client node running nomad
      val server =
        new NettyServerComponents with BuiltInComponents {
          lazy val router: Router = Router.from {
            case GET(p"/v1/client/stats") =>
              Action {
                Results.Ok(
                  Json.parse(
                    """{
                    |  "AllocDirStats": {
                    |    "Available": 60438560768,
                    |    "Device": "",
                    |    "InodesUsedPercent": 25.660133361816406,
                    |    "Mountpoint": "",
                    |    "Size": 135148244992,
                    |    "Used": 67820965888,
                    |    "UsedPercent": 50.18264639101648
                    |  },
                    |  "CPU": [
                    |    {
                    |      "CPU": "cpu0",
                    |      "Idle": 98.0000000372529,
                    |      "System": 1.0000000046566129,
                    |      "Total": 2.0000000093132257,
                    |      "User": 1.0000000046566129
                    |    },
                    |    {
                    |      "CPU": "cpu1",
                    |      "Idle": 100,
                    |      "System": 0,
                    |      "Total": 0,
                    |      "User": 0
                    |    }
                    |  ],
                    |  "CPUTicksConsumed": 45.94000021392479,
                    |  "DiskStats": [
                    |    {
                    |      "Available": 60438560768,
                    |      "Device": "/dev/vda1",
                    |      "InodesUsedPercent": 25.660133361816406,
                    |      "Mountpoint": "/",
                    |      "Size": 135148244992,
                    |      "Used": 67820965888,
                    |      "UsedPercent": 50.18264639101648
                    |    },
                    |    {
                    |      "Available": 60438560768,
                    |      "Device": "/dev/vda1",
                    |      "InodesUsedPercent": 25.660133361816406,
                    |      "Mountpoint": "/var/lib/docker/aufs",
                    |      "Size": 135148244992,
                    |      "Used": 67820965888,
                    |      "UsedPercent": 50.18264639101648
                    |    }
                    |  ],
                    |  "Memory": {
                    |    "Available": 15694356480,
                    |    "Free": 809086976,
                    |    "Total": 16824614912,
                    |    "Used": 1130258432
                    |  },
                    |  "Timestamp": 1537952757466164700,
                    |  "Uptime": 21837925
                    |}""".stripMargin
                  )
                )
              }
            case GET(p"/v1/client/allocation/51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29/stats") =>
              Action {
                Results.Ok(
                  Json.parse(
                    """{
                    |  "ResourceUsage": {
                    |    "CpuStats": {
                    |      "Measured": [
                    |        "Throttled Periods",
                    |        "Throttled Time",
                    |        "Percent"
                    |      ],
                    |      "Percent": 0.6793430769230769,
                    |      "SystemMode": 0,
                    |      "ThrottledPeriods": 0,
                    |      "ThrottledTime": 0,
                    |      "TotalTicks": 15.604510476923076,
                    |      "UserMode": 301.95082881728126
                    |    },
                    |    "MemoryStats": {
                    |      "Cache": 8134656,
                    |      "KernelMaxUsage": 0,
                    |      "KernelUsage": 0,
                    |      "MaxUsage": 18333696,
                    |      "Measured": [
                    |        "RSS",
                    |        "Cache",
                    |        "Swap",
                    |        "Max Usage"
                    |      ],
                    |      "RSS": 9945088,
                    |      "Swap": 0
                    |    }
                    |  },
                    |  "Tasks": {
                    |    "nooe-vault": {
                    |      "Pids": null,
                    |      "ResourceUsage": {
                    |        "CpuStats": {
                    |          "Measured": [
                    |            "Throttled Periods",
                    |            "Throttled Time",
                    |            "Percent"
                    |          ],
                    |          "Percent": 0.6793430769230769,
                    |          "SystemMode": 0,
                    |          "ThrottledPeriods": 0,
                    |          "ThrottledTime": 0,
                    |          "TotalTicks": 15.604510476923076,
                    |          "UserMode": 301.95082881728126
                    |        },
                    |        "MemoryStats": {
                    |          "Cache": 8134656,
                    |          "KernelMaxUsage": 0,
                    |          "KernelUsage": 0,
                    |          "MaxUsage": 18333696,
                    |          "Measured": [
                    |            "RSS",
                    |            "Cache",
                    |            "Swap",
                    |            "Max Usage"
                    |          ],
                    |          "RSS": 9945088,
                    |          "Swap": 0
                    |        }
                    |      },
                    |      "Timestamp": 1537958516820011500
                    |    }
                    |  },
                    |  "Timestamp": 1537958516820011500
                    |}""".stripMargin
                  )
                )
              }
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
        case GET(p"/v1/node/b9747124-1854-64a5-522b-1f1f32747eda/allocations") =>
          Action {
            Results.Ok(
              Json.parse(
                // some unused fields have been shortened to {} because they are not parsed.
                s"""[
                   |  {
                   |    "ID": "51f1cfce-0a77-6bc5-57d4-1ffc98c7ac29",
                   |    "EvalID": "ed054cc6-5fd6-1683-8d80-3446dd3380c9",
                   |    "Name": "nooe-vault.nooe-vault[0]",
                   |    "NodeID": "b9747124-1854-64a5-522b-1f1f32747eda",
                   |    "JobID": "nooe-vault",
                   |    "Job": {},
                   |    "TaskGroup": "nooe-vault",
                   |    "Resources": {
                   |      "CPU": 500,
                   |      "MemoryMB": 1024,
                   |      "DiskMB": 300,
                   |      "IOPS": 0,
                   |      "Networks": [
                   |        {
                   |          "Device": "eth0",
                   |          "CIDR": "",
                   |          "IP": "10.250.24.142",
                   |          "MBits": 100,
                   |          "ReservedPorts": [
                   |            {
                   |              "Label": "vault",
                   |              "Value": 8200
                   |            }
                   |          ],
                   |          "DynamicPorts": null
                   |        }
                   |      ]
                   |    },
                   |    "SharedResources": {
                   |      "CPU": 0,
                   |      "MemoryMB": 0,
                   |      "DiskMB": 300,
                   |      "IOPS": 0,
                   |      "Networks": null
                   |    },
                   |    "TaskResources": {
                   |      "nooe-vault": {
                   |        "CPU": 500,
                   |        "MemoryMB": 1024,
                   |        "DiskMB": 0,
                   |        "IOPS": 0,
                   |        "Networks": [
                   |          {
                   |            "Device": "eth0",
                   |            "CIDR": "",
                   |            "IP": "10.250.24.142",
                   |            "MBits": 100,
                   |            "ReservedPorts": [
                   |              {
                   |                "Label": "vault",
                   |                "Value": 8200
                   |              }
                   |            ],
                   |            "DynamicPorts": null
                   |          }
                   |        ]
                   |      }
                   |    },
                   |    "Metrics": {},
                   |    "DesiredStatus": "run",
                   |    "DesiredDescription": "",
                   |    "ClientStatus": "running",
                   |    "ClientDescription": "",
                   |    "TaskStates": {},
                   |    "PreviousAllocation": "776a3bb8-c6f6-965d-d88e-fe780b767b28",
                   |    "CreateIndex": 14582058,
                   |    "ModifyIndex": 14582061,
                   |    "AllocModifyIndex": 14582058,
                   |    "CreateTime": 1536848890630352000
                   |  },
                   |  {
                   |    "ID": "b2134fc5-7cf9-d7f1-5d92-77d024e6ed98",
                   |    "EvalID": "bbdfa740-993b-6ace-616a-664d13ed0888",
                   |    "Name": "nooe-luigi-flow-example/periodic-1518701400.nooe-luigi-flow-example[0]",
                   |    "NodeID": "b9747124-1854-64a5-522b-1f1f32747eda",
                   |    "JobID": "nooe-luigi-flow-example/periodic-1518701400",
                   |    "Job": {},
                   |    "TaskGroup": "nooe-luigi-flow-example",
                   |    "Resources": {
                   |      "CPU": 500,
                   |      "MemoryMB": 512,
                   |      "DiskMB": 300,
                   |      "IOPS": 0,
                   |      "Networks": [
                   |        {
                   |          "Device": "eth0",
                   |          "CIDR": "",
                   |          "IP": "10.250.24.142",
                   |          "MBits": 100,
                   |          "ReservedPorts": null,
                   |          "DynamicPorts": null
                   |        }
                   |      ]
                   |    },
                   |    "SharedResources": {
                   |      "CPU": 0,
                   |      "MemoryMB": 0,
                   |      "DiskMB": 300,
                   |      "IOPS": 0,
                   |      "Networks": null
                   |    },
                   |    "TaskResources": {
                   |      "nooe-luigi-flow-example": {
                   |        "CPU": 500,
                   |        "MemoryMB": 512,
                   |        "DiskMB": 0,
                   |        "IOPS": 0,
                   |        "Networks": [
                   |          {
                   |            "Device": "eth0",
                   |            "CIDR": "",
                   |            "IP": "10.250.24.142",
                   |            "MBits": 100,
                   |            "ReservedPorts": null,
                   |            "DynamicPorts": null
                   |          }
                   |        ]
                   |      }
                   |    },
                   |    "Metrics": {},
                   |    "DesiredStatus": "run",
                   |    "DesiredDescription": "",
                   |    "ClientStatus": "complete",
                   |    "ClientDescription": "",
                   |    "TaskStates": {},
                   |    "PreviousAllocation": "",
                   |    "CreateIndex": 5467450,
                   |    "ModifyIndex": 5508801,
                   |    "AllocModifyIndex": 5467450,
                   |    "CreateTime": 1518701400124658700
                   |  }
                   |]""".stripMargin)
            )
          }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val configuration = NomadConfiguration(url = s"http://localhost:$port", "NOMAD_BROCCOLI_TOKEN")
          val nomadService = new NomadService(configuration, client)
          val result =
            Await.result(nomadService.getNodeResources(new Account("nooe-admin", "nooe-*", Role.Administrator)),
                         Duration(5, TimeUnit.SECONDS))
          server.stop() // stop test server
          result === List(nodesResources)
        }
      }
    }
  }
}
