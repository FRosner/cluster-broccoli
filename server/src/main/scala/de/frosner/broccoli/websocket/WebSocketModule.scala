package de.frosner.broccoli.websocket

import com.google.inject.{AbstractModule, Provides, Singleton}
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule
import play.api.cache.SyncCacheApi

import scala.concurrent.ExecutionContext

/**
  * Configure websockets for Broccoli.
  */
class WebSocketModule extends AbstractModule with ScalaModule {
  override def configure(): Unit =
    bind[WebSocketMessageHandler].to[CachedBroccoliMessageHandler]

  /**
    * Provide the websocket configuration of Broccoli.
    *
    * @param configuration Broccoli's configuration
    * @return The websocket part of Broccoli's configuration
    */
  @Provides
  def providesWebSocketConfiguration(configuration: BroccoliConfiguration): WebSocketConfiguration =
    configuration.webSocket

  /**
    * Provide the cached websocket message handler for Broccoli.
    *
    * @param underlying The underlying Broccoli message handler
    * @param cacheApi The cache to use for web-socket messages
    * @param webSocketConfiguration The configuration for websockets
    * @return A web socket message handler that caches some responses
    */
  @Provides
  @Singleton
  def providesCachedWebSocketMessageHandler(
      underlying: BroccoliMessageHandler,
      cacheApi: SyncCacheApi,
      webSocketConfiguration: WebSocketConfiguration,
      executionContext: ExecutionContext
  ) = new CachedBroccoliMessageHandler(underlying, cacheApi, webSocketConfiguration.cacheTimeout)(executionContext)
}
