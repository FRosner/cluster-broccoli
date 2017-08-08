package de.frosner.broccoli.nomad

import de.frosner.broccoli.test.contexts.WSClientContext
import de.frosner.broccoli.test.contexts.docker.BroccoliDockerContext
import de.frosner.broccoli.test.contexts.docker.BroccoliTestService.{Broccoli, Nomad}
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.blocking
import scala.concurrent.duration._

class NomadHttpClientIntegrationSpec
    extends Specification
    with WSClientContext
    with BroccoliDockerContext
    with ExecutionEnvironment {

  /**
    * Start Broccoli and Nomad for our tests.  We need Broccoli to spawn instances, and Nomad to test the client.
    */
  override def broccoliDockerConfig: BroccoliDockerContext.Configuration =
    BroccoliDockerContext.Configuration.services(Broccoli, Nomad)

  private val baseUrl = "http://localhost:4646"
  private val broccoliURL = "http://localhost:9000"

  override def is(implicit executionEnv: ExecutionEnv): Any =
    "The NomadHttpClient" should {
      "get allocations for a running nomad job" >> { wsClient: WSClient =>
        // Generate a random identifier for the instance
        val identifier = Gen.resize(10, Gen.identifier).sample.get
        val client = new NomadHttpClient(baseUrl, wsClient)
        (for {
          // Create and start a simple instance to look at it's allocations
          _ <- wsClient
            .url(s"$broccoliURL/api/v1/instances")
            .post(
              Json.obj("templateId" -> "http-server",
                       "parameters" -> Json.obj(
                         "id" -> identifier
                       )))
            .map(response => {
              // Ensure that the
              response.status must beEqualTo(201)
              response
            })
          _ <- wsClient
            .url(s"$broccoliURL/api/v1/instances/$identifier")
            .post(Json.obj("status" -> "running"))
            .map(response => {
              response.status must beEqualTo(200)
              // Wait until the service is up
              blocking(Thread.sleep(1.seconds.toMillis))
              response
            })
          allocations <- client.getAllocationsForJob(identifier)
        } yield {
          (allocations.jobId === identifier) and (allocations.payload must have length 1)
        }).await(5, broccoliDockerConfig.startupPatience + 2.seconds)
      }
    }
}
