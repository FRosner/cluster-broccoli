package de.frosner.broccoli.instances

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad
import de.frosner.broccoli.nomad.NomadClient
import de.frosner.broccoli.nomad.models.{Allocation, Job, NomadError, WithId}
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment

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
      "get instance tasks from nomad" in prop { (user: UserAccount, id: String, allocations: List[Allocation]) =>
        val client = mock[NomadClient]

        client
          .getAllocationsForJob(shapeless.tag[Job.Id](id))
          .returns(EitherT.pure[Future, NomadError](WithId(id, allocations)))

        {
          for {
            // Make sure the user really has access to the instance by putting the ID into the instance regex
            result <- new NomadInstances(client).getInstanceTasks(user.copy(instanceRegex = id))(id).value
          } yield
            result must beRight[InstanceTasks] { instanceTasks: InstanceTasks =>
              val tasks = allocations.flatMap(_.taskStates).groupBy(_._1).mapValues(_.map(_._2))
              (instanceTasks.instanceId must beEqualTo(id)) and
                (instanceTasks.tasks must have length tasks.size) and
                (instanceTasks.tasks.map(_.name) must containTheSameElementsAs(tasks.keys.toSeq))
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
