package de.frosner.broccoli.websocket

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.models._
import de.frosner.broccoli.services.InstanceService
import de.frosner.broccoli.nomad
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.ExecutionEnvironment

import scala.concurrent.Future

class BroccoliMessageHandlerSpec
    extends Specification
    with ScalaCheck
    with Mockito
    with nomad.ModelArbitraries
    with ModelArbitraries {

  "The Broccoli Message Handler" should {
    "send back instance tasks" in { implicit ee: ExecutionEnv =>
      prop { (account: Account, id: String, tasks: List[Task]) =>
        val service = mock[InstanceService]
        val instanceTasks = InstanceTasks(id, tasks)
        service.getInstanceTasks(account)(id) returns EitherT.pure[Future, InstanceError](instanceTasks)

        val outgoingMessage = new BroccoliMessageHandler(service)
          .processMessage(account)(IncomingMessage.GetInstanceTasks(id))

        outgoingMessage must beEqualTo(OutgoingMessage.GetInstanceTasksSuccess(instanceTasks)).await
      }.setGen2(Gen.identifier)
    }

    "send back an error if instance tasks failed" in { implicit ee: ExecutionEnv =>
      prop { (account: Account, id: String, error: InstanceError) =>
        val service = mock[InstanceService]
        service.getInstanceTasks(account)(id) returns EitherT.leftT[Future, InstanceTasks](error)

        val outgoingMessage = new BroccoliMessageHandler(service)
          .processMessage(account)(IncomingMessage.GetInstanceTasks(id))

        outgoingMessage must beEqualTo(OutgoingMessage.GetInstanceTasksError(error)).await
      }.setGen2(Gen.identifier)
    }
  }
}
