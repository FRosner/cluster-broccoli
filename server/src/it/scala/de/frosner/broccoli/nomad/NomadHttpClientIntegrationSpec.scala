package de.frosner.broccoli.nomad

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import de.frosner.broccoli.test.contexts.WSClientContext
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.ForEach
import org.specs2.specification.mutable.ExecutionEnvironment
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

class NomadHttpClientIntegrationSpec extends Specification with WSClientContext with ExecutionEnvironment {
  private val baseUrl = "http://localhost:4646"

  override def is(implicit executionEnv: ExecutionEnv): Any =
    "The NomadHttpClient" should {
      "get allocations for a running nomad job" >> { wsClient: WSClient =>
        val client = new NomadHttpClient(baseUrl, wsClient)
        (for {
          allocations <- client.getAllocationsForJob("test")
        } yield {
          (allocations.jobId === "test") and (allocations.payload must have length (1))
        }).await
      }
    }
}
