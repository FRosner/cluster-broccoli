package de.frosner.broccoli.instances

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad
import de.frosner.broccoli.nomad.models._
import de.frosner.broccoli.nomad.{NomadClient, NomadConfiguration, NomadNodeClient}
import de.frosner.broccoli.services.InstanceService
import org.mockito.Matchers
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment
import shapeless.tag.@@

import scala.collection.immutable
import scala.concurrent.Future

class NomadInstancesSpec
    extends Specification
    with ScalaCheck
    with Mockito
    with ModelArbitraries
    with nomad.ModelArbitraries
    with ExecutionEnvironment {

  val dummyInstance = InstanceWithStatus(
    instance = Instance(
      id = "id",
      template = Template(
        id = "id",
        template = "{{id}}",
        description = "description",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))
      ),
      parameterValues = Map("id" -> StringParameterValue("id"))
    ),
    status = JobStatus.Running,
    services = Seq.empty,
    periodicRuns = Seq.empty
  )

  implicit val nomadConfiguration: NomadConfiguration =
    NomadConfiguration(url = s"http://localhost:4646", "NOMAD_BROCCOLI_TOKEN", namespacesEnabled = false, "oe")

  def instanceServiceWith(id: String, instance: Option[InstanceWithStatus]): InstanceService =
    mock[InstanceService]
      .getInstance(Matchers.any[String @@ Job.Id])
      .returns(instance.map(i => i.copy(instance = i.instance.copy(id = id))))

  override def is(implicit executionEnv: ExecutionEnv): Any =
    "NomadInstances" should {
      "get instance tasks from nomad" in prop {
        (user: Account, id: String, allocations: List[Allocation], resourceUsage: ResourceUsage) =>
          val instanceService = instanceServiceWith(id, Some(dummyInstance))
          val client = mock[NomadClient]
          val nodeClient = mock[NomadNodeClient]

          nodeClient
            .getAllocationStats(Matchers.any[String @@ Allocation.Id](), Matchers.eq(None))
            .returns(EitherT.pure(AllocationStats(resourceUsage, Map.empty)))

          client.allocationNodeClient(Matchers.any[Allocation]).returns(EitherT.pure(nodeClient))
          client.getJob(shapeless.tag[Job.Id](id), None).returns(EitherT.pure(Job(Seq.empty)))
          client
            .getAllocationsForJob(shapeless.tag[Job.Id](id), None)
            .returns(EitherT.pure(WithId(id, allocations)))

          {
            for {
              // Make sure the user really has access to the instance by putting the ID into the instance regex
              result <- new NomadInstances(client, instanceService)
                .getInstanceTasks(user.copy(instanceRegex = id))(id)
                .value
            } yield
              result must beRight[InstanceTasks] { instanceTasks: InstanceTasks =>
                (instanceTasks.instanceId must beEqualTo(id)) and
                  (instanceTasks.allocatedTasks must have length allocations.map(_.taskStates.size).sum) and
                  (instanceTasks.allocatedTasks.map(_.taskName) must containTheSameElementsAs(
                    allocations.flatMap(_.taskStates.keys))) and
                  (instanceTasks.allocatedTasks must contain((task: AllocatedTask) =>
                    (task.resources.cpuUsed must beNone) and (task.resources.memoryUsed must beNone)).foreach)
              }
          }.await
        // Reduce the size of the generated values; we don't need to check this against huge allocation lists
      }.setGen2(Gen.identifier.label("id")).set(maxSize = 10)

      "include resources in instance tasks" in todo

      "return empty instance tasks if the job can't be found" in prop { (user: Account, id: String) =>
        val instanceService = instanceServiceWith(id, Some(dummyInstance))
        val client = mock[NomadClient]

        client.getJob(shapeless.tag[Job.Id](id), None).returns(EitherT.leftT(NomadError.NotFound))
        client.getAllocationsForJob(shapeless.tag[Job.Id](id), None).returns(EitherT.pure(WithId(id, List.empty)))

        {
          for {
            result <- new NomadInstances(client, instanceService)
              .getInstanceTasks(user.copy(instanceRegex = id))(id)
              .value
          } yield result shouldEqual Right(InstanceTasks(id, List.empty, Map.empty))
        }.await
      }.setGen2(Gen.identifier)

      "fail to get instance tasks when the user may not access the instance" in prop { (user: Account, id: String) =>
        (!id.matches(user.instanceRegex)) ==> {
          for {
            result <- new NomadInstances(mock[NomadClient], mock[InstanceService]).getInstanceTasks(user)(id).value
          } yield {
            result must beLeft[InstanceError](InstanceError.UserRegexDenied(id, user.instanceRegex))
          }
        }.await
      }.set(minTestsOk = 5)

      "fail to get instance tasks when Nomad fails" in prop { (user: Account, id: String, error: NomadError) =>
        val client = mock[NomadClient]
        val instanceService = instanceServiceWith(id, Some(dummyInstance))

        client
          .getAllocationsForJob(shapeless.tag[Job.Id](id), None)
          .returns(EitherT.leftT(error))

        {
          for {
            result <- new NomadInstances(client, instanceService).getInstanceTasks(user)(id).value
          } yield {
            result must beLeft[InstanceError]
          }
        }.await
      }

      "fail to get instance tasks when instance does not belong to Broccoli" in prop {
        (user: Account, id: String, error: NomadError) =>
          val client = mock[NomadClient]
          val instanceService = instanceServiceWith(id, None)

          {
            for {
              result <- new NomadInstances(client, instanceService)
                .getInstanceTasks(user.copy(instanceRegex = id))(id)
                .value
            } yield {
              result shouldEqual Left(InstanceError.NotFound(id))
            }
          }.await
      }.setGen2(Gen.identifier)

      "fail to get instance logs when instance does not belong to Broccoli" in prop {
        (user: Account, id: String, error: NomadError) =>
          val client = mock[NomadClient]
          val instanceService = instanceServiceWith(id, None)

          {
            for {
              result <- new NomadInstances(client, instanceService)
                .getAllocationLog(user.copy(instanceRegex = id))(
                  instanceId = id,
                  allocationId = shapeless.tag[Allocation.Id]("allocId"),
                  taskName = shapeless.tag[Task.Name]("task"),
                  logKind = LogStreamKind.StdOut,
                  offset = None
                )
                .value
            } yield {
              result shouldEqual Left(InstanceError.NotFound(id))
            }
          }.await
      }.setGen2(Gen.identifier)

      "fail to get periodic instance logs when instance does not belong to Broccoli" in prop {
        (user: Account, id: String, error: NomadError) =>
          val client = mock[NomadClient]
          val instanceService = instanceServiceWith(id, None)

          {
            for {
              result <- new NomadInstances(client, instanceService)
                .getPeriodicJobAllocationLog(user.copy(instanceRegex = id))(
                  instanceId = id,
                  periodicJobId = id + "/periodic",
                  allocationId = shapeless.tag[Allocation.Id]("allocId"),
                  taskName = shapeless.tag[Task.Name]("task"),
                  logKind = LogStreamKind.StdOut,
                  offset = None
                )
                .value
            } yield {
              result shouldEqual Left(InstanceError.NotFound(id))
            }
          }.await
      }.setGen2(Gen.identifier)

      "fail to get periodic instance logs when periodic run does not belong to instance" in prop {
        (user: Account, id: String, error: NomadError) =>
          val client = mock[NomadClient]
          val instanceService = instanceServiceWith(id, Some(dummyInstance))

          {
            for {
              result <- new NomadInstances(client, instanceService)
                .getPeriodicJobAllocationLog(user.copy(instanceRegex = id))(
                  instanceId = id,
                  periodicJobId = id + "/periodic",
                  allocationId = shapeless.tag[Allocation.Id]("allocId"),
                  taskName = shapeless.tag[Task.Name]("task"),
                  logKind = LogStreamKind.StdOut,
                  offset = None
                )
                .value
            } yield {
              result shouldEqual Left(InstanceError.NotFound(id + "/periodic"))
            }
          }.await
      }.setGen2(Gen.identifier)
    }
}
