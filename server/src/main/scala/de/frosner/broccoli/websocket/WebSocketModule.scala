package de.frosner.broccoli.websocket

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

/**
  * Configure websockets for Broccoli.
  */
class WebSocketModule extends AbstractModule with ScalaModule {
  override def configure(): Unit =
    bind[WebSocketMessageHandler].to[CachedBroccoliMessageHandler]

}
