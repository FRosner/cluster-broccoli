package de.frosner.broccoli.websocket

import javax.inject.Inject

import cats.syntax.either._
import cats.instances.future._
import de.frosner.broccoli.auth.UserAccount
import de.frosner.broccoli.controllers.InstanceController
import de.frosner.broccoli.instances.NomadInstances
import de.frosner.broccoli.services.InstanceService
import de.frosner.broccoli.websocket.OutgoingMessage._
import de.frosner.broccoli.websocket.IncomingMessage._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Process websocket messages.
  */
trait WebSocketMessageHandler {

  /**
    * Process an incoming websocket message.
    *
    * @param user The user who's sending the messages
    * @param incomingMessage An incoming message
    * @return An outgoing message to send in response
    */
  def processMessage(user: UserAccount)(incomingMessage: IncomingMessage): Future[OutgoingMessage]
}

/**
  * Process broccoli's websocket messages.
  */
class BroccoliMessageHandler @Inject()(instances: NomadInstances, instanceService: InstanceService)(
    implicit ec: ExecutionContext)
    extends WebSocketMessageHandler {

  override def processMessage(user: UserAccount)(incomingMessage: IncomingMessage): Future[OutgoingMessage] =
    incomingMessage match {
      case AddInstance(instanceCreation) =>
        InstanceController
          .create(instanceCreation, user, instanceService)
          .toEitherT
          .fold(AddInstanceError, AddInstanceSuccess)
      case DeleteInstance(instanceId) =>
        InstanceController
          .delete(instanceId, user, instanceService)
          .toEitherT
          .fold(DeleteInstanceError, DeleteInstanceSuccess)
      case UpdateInstance(instanceUpdate) =>
        InstanceController
          .update(instanceUpdate.instanceId.get, instanceUpdate, user, instanceService)
          .toEitherT
          .fold(UpdateInstanceError, UpdateInstanceSuccess)
      case GetInstanceTasks(instanceId) =>
        instances
          .getInstanceTasks(user)(instanceId)
          .fold(GetInstanceTasksError, GetInstanceTasksSuccess)
    }
}
