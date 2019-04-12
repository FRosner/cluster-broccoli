package de.frosner.broccoli.test.contexts

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.ForEach
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

/**
  * Provides a WSClient instance to tests.
  *
  * Requires the ExecutionEnvironment to be mixed in.
  */
trait WSClientContext extends ForEach[WSClient] {

  override protected def foreach[R: AsResult](f: (WSClient) => R): Result = {
    implicit val actorSystem = ActorSystem("nomad-http-client")
    try {
      implicit val materializer = ActorMaterializer()
      val client: WSClient = AhcWSClient()
      try AsResult(f(client))
      finally client.close()
    } finally {
      actorSystem.terminate()
    }
  }
}
