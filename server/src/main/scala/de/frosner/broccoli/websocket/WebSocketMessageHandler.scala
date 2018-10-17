package de.frosner.broccoli.websocket

import javax.inject.Inject
import cats.instances.future._
import cats.syntax.either._
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.controllers.InstanceController
import de.frosner.broccoli.instances.NomadInstances
import de.frosner.broccoli.services.{InstanceService, NomadService}
import de.frosner.broccoli.websocket.IncomingMessage._
import de.frosner.broccoli.websocket.OutgoingMessage._
import play.api.Logger
import play.api.cache.CacheApi

import scala.concurrent.duration._
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
  def processMessage(user: Account)(incomingMessage: IncomingMessage): Future[OutgoingMessage]
}

/**
  * Process broccoli's websocket messages.
  */
class BroccoliMessageHandler @Inject()(
    instances: NomadInstances,
    instanceService: InstanceService,
    nomadService: NomadService
)(implicit ec: ExecutionContext)
    extends WebSocketMessageHandler {

  override def processMessage(user: Account)(incomingMessage: IncomingMessage): Future[OutgoingMessage] =
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
          .fold(GetInstanceTasksError(instanceId, _), GetInstanceTasksSuccess)
      case GetResources() =>
        nomadService.getNodeResources(user).map(OutgoingMessage.ListResources)
    }
}

/**
  * Cache some messages from the Broccoli message handler.
  *
  * Reduces load on Nomad.
  *
  * @param underlying The underlying message handler.
  * @param cache The cache to use for responses
  */
class CachedBroccoliMessageHandler @Inject()(
    underlying: BroccoliMessageHandler,
    cache: CacheApi,
    cacheTimeout: Duration
)(implicit ec: ExecutionContext)
    extends WebSocketMessageHandler {

  private val timeout: Duration = 3.seconds

  /**
    * Cache a message.
    *
    * If a message exists in cache return it, otherwise invoke the underlying handler and cache the result upon
    * completion.
    *
    * @param user The user, to compute the cache key from
    * @param messageKey The key for the specific message
    * @param message The incoming message
    * @return The cache value or the result of the underlying handler
    */
  private def cachedMessage(user: Account, messageKey: String)(message: IncomingMessage): Future[OutgoingMessage] = {
    val cacheKey = s"${user.name}.$messageKey"
    cache
      .get[OutgoingMessage](cacheKey)
      .map(Future.successful)
      .getOrElse {
        val result = underlying.processMessage(user)(message)
        result.onSuccess {
          case outgoingMessage =>
            cache.set(cacheKey, outgoingMessage, timeout)
        }
        result
      }
  }

  override def processMessage(user: Account)(incomingMessage: IncomingMessage): Future[OutgoingMessage] =
    incomingMessage match {
      // Only cache side-effect free "get" messages!
      case message @ GetInstanceTasks(instanceId) => cachedMessage(user, instanceId)(message)
      // For all other messages that might trigger side-effects call out to the underlying handler directly
      case message => underlying.processMessage(user)(message)
    }
}
