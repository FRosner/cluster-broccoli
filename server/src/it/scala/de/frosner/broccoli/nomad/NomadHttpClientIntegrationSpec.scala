package de.frosner.broccoli.nomad

import cats.instances.future._
import io.lemonlabs.uri.Url
import io.lemonlabs.uri.dsl._
import de.frosner.broccoli.nomad.models.{Allocation, Job, WithId}
import de.frosner.broccoli.test.contexts.WSClientContext
import de.frosner.broccoli.test.contexts.docker.BroccoliDockerContext
import de.frosner.broccoli.test.contexts.docker.BroccoliTestService.{Broccoli, Nomad}
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.collection.immutable
import scala.concurrent.blocking
import scala.concurrent.duration._

class NomadHttpClientIntegrationSpec(implicit ee: ExecutionEnv)
    extends Specification
    with WSClientContext
    with BroccoliDockerContext {

  /**
    * Start Broccoli and Nomad for our tests.  We need Broccoli to spawn instances, and Nomad to test the client.
    */
  override def broccoliDockerConfig: BroccoliDockerContext.Configuration =
    BroccoliDockerContext.Configuration.services(Broccoli, Nomad)

  private val broccoliApi = "http://localhost:9000/api/v1"

  "The NomadHttpClient" should {
    "get allocations for a running nomad job" >> { wsClient: WSClient =>
      // Generate a random identifier for the instance
      val identifier = Gen.resize(10, Gen.identifier).sample.get
      val client = new NomadHttpClient(Url.parse("http://localhost:4646"), "NOMAD_BROCCOLI_TOKEN", wsClient)
      (for {
        // Create and start a simple instance to look at it's allocations
        _ <- wsClient
          .url(broccoliApi / "instances")
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
          .url(broccoliApi / "instances" / identifier)
          .post(Json.obj("status" -> "running"))
          .map(response => {
            response.status must beEqualTo(200)
            // Wait until the service is up
            blocking(Thread.sleep(1.seconds.toMillis))
            response
          })
        allocations <- client.getAllocationsForJob(shapeless.tag[Job.Id](identifier), None).value
      } yield {
        allocations must beRight(
          (v: WithId[immutable.Seq[Allocation]]) => (v.jobId === identifier) and (v.payload must have length 1))
      }).await(5, broccoliDockerConfig.startupPatience + 10.seconds)
    }
  }
}
