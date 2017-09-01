package de.frosner.broccoli.instances

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad
import de.frosner.broccoli.nomad.{NomadClient, NomadNodeClient}
import de.frosner.broccoli.nomad.models._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.mockito.Matchers
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

  override def is(implicit executionEnv: ExecutionEnv): Any =
    "NomadInstances" should {
      "get instance tasks from nomad" in prop {
        (user: UserAccount, id: String, allocations: List[Allocation], resourceUsage: ResourceUsage) =>
          val client = mock[NomadClient]
          val nodeClient = mock[NomadNodeClient]

          nodeClient
            .getAllocationStats(Matchers.any[String @@ Allocation.Id]())
            .returns(EitherT.pure[Future, NomadError](AllocationStats(resourceUsage, Map.empty)))

          client.allocationNodeClient(Matchers.any[Allocation]).returns(EitherT.pure[Future, NomadError](nodeClient))

          client
            .getAllocationsForJob(shapeless.tag[Job.Id](id))
            .returns(EitherT.pure[Future, NomadError](WithId(id, allocations)))

          {
            for {
              // Make sure the user really has access to the instance by putting the ID into the instance regex
              result <- new NomadInstances(client).getInstanceTasks(user.copy(instanceRegex = id))(id).value
            } yield
              result must beRight[InstanceTasks] { instanceTasks: InstanceTasks =>
                (instanceTasks.instanceId must beEqualTo(id)) and
                  (instanceTasks.allocatedTasks must have length allocations.map(_.taskStates.size).sum) and
                  (instanceTasks.allocatedTasks.map(_.taskName) must containTheSameElementsAs(
                    allocations.flatMap(_.taskStates.keys))) and
                  (instanceTasks.allocatedTasks must contain(
                    (task: AllocatedTask) => (task.cpuTicksUsed must beNone) and (task.memoryUsed must beNone)).foreach)
              }
          }.await
        // Reduce the size of the generated values; we don't need to check this against huge allocation lists
      }.setGen2(Gen.identifier.label("id")).set(maxSize = 10)

      "fail to get instance tasks from nomad when the user may not access the instance" in prop {
        (user: UserAccount, id: String) =>
          (!id.matches(user.instanceRegex)) ==> {
            for {
              result <- new NomadInstances(mock[NomadClient]).getInstanceTasks(user)(id).value
            } yield {
              result must beLeft[InstanceError](InstanceError.UserRegexDenied(id, user.instanceRegex))
            }
          }.await
      }.set(minTestsOk = 5)

      "fail when Nomad fails" in prop { (user: UserAccount, id: String, error: NomadError) =>
        val client = mock[NomadClient]

        client
          .getAllocationsForJob(shapeless.tag[Job.Id](id))
          .returns(EitherT.leftT[Future, WithId[immutable.Seq[Allocation]]](error))

        {
          for {
            result <- new NomadInstances(client).getInstanceTasks(user)(id).value
          } yield {
            result must beLeft[InstanceError]
          }
        }.await
      }
    }
}