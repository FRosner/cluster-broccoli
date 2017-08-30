package de.frosner.broccoli.websocket

import scala.concurrent.duration.Duration

/**
  * Configuration for Broccoli's websocket
  *
  * @param cacheTimeout The timeout for the websocket message cache
  */
final case class WebSocketConfiguration(cacheTimeout: Duration)
