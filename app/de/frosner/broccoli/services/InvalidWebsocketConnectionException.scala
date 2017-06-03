package de.frosner.broccoli.services

case class InvalidWebsocketConnectionException(id: String, connections: Iterable[String])
  extends Exception(s"Connection $id is not in the pool of websocket connections: ${connections.mkString(", ")}")
