package de.frosner.broccoli.websocket

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.instances.NomadInstances
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad
import de.frosner.broccoli.services.InstanceService
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Future

class BroccoliMessageHandlerSpec
    extends Specification
    with ScalaCheck
    with Mockito
    with nomad.ModelArbitraries
    with ModelArbitraries {

  "The Broccoli Message Handler" should {
    "send back instance tasks" in { implicit ee: ExecutionEnv =>
      prop {
        (account: Account,
         id: String,
         tasks: List[AllocatedTask],
         periodicRunTasks: Map[String, List[AllocatedTask]]) =>
          val instances = mock[NomadInstances]
          val instanceTasks = InstanceTasks(id, tasks, periodicRunTasks)
          instances.getInstanceTasks(account)(id) returns EitherT.pure[Future, InstanceError](instanceTasks)

          val outgoingMessage = new BroccoliMessageHandler(instances, mock[InstanceService])
            .processMessage(account)(IncomingMessage.GetInstanceTasks(id))

          outgoingMessage must beEqualTo(OutgoingMessage.GetInstanceTasksSuccess(instanceTasks)).await
      }.setGen2(Gen.identifier)
    }

    "send back an error if instance tasks failed" in { implicit ee: ExecutionEnv =>
      prop { (account: Account, id: String, error: InstanceError) =>
        val instances = mock[NomadInstances]
        instances.getInstanceTasks(account)(id) returns EitherT.leftT[Future, InstanceTasks](error)

        val outgoingMessage = new BroccoliMessageHandler(instances, mock[InstanceService])
          .processMessage(account)(IncomingMessage.GetInstanceTasks(id))

        outgoingMessage must beEqualTo(OutgoingMessage.GetInstanceTasksError(id, error)).await
      }.setGen2(Gen.identifier)
    }
  }
}
