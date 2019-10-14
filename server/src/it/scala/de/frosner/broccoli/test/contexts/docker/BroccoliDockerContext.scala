package de.frosner.broccoli.test.contexts.docker

import cats.instances.all._
import cats.syntax.all._
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.AroundEach
import shapeless.tag
import shapeless.tag.@@

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process._
import scala.util.{Failure, Success, Try}

/**
  * Type tag for container handles
  */
trait ContainerHandle

/**
  * Start configured services from the Broccoli Test image before the test and stop-kill all containers after every
  * test.
  *
  * Note that docker typically requires root privileges so this class uses "sudo" to start docker images.  To run tests
  * seamlessly configure sudo to not ask for password for docker commands.
  */
trait BroccoliDockerContext extends AroundEach {

  /**
    * Configuration for the broccoli docker image.
    */
  def broccoliDockerConfig: BroccoliDockerContext.Configuration

  override def around[T: AsResult](t: => T): Result = {
    val containers = spawnContainers()
    try {
      // Wait for the services to come up
      Thread.sleep(broccoliDockerConfig.startupPatience.toMillis)
      AsResult(t)
    } finally {
      stopContainers(containers)
    }
  }

  private def spawnContainers(): List[String @@ ContainerHandle] = {
    val handles = broccoliDockerConfig.services.toList.map(spawnContainer)
    // Go through all started containers; throw the first exception seen, or return all handles if successful
    handles.sequence[Try, String @@ ContainerHandle] match {
      case Success(h)   => h
      case Failure(exc) =>
        // Stop all running containers and then throw the error
        stopContainers(handles.collect {
          case Success(h) => h
        })
        throw exc
    }
  }

  private def stopContainers(handles: List[String @@ ContainerHandle]): Unit =
    // Stop all containers, throwing the first exception seen
    handles.map(stopContainer).sequence_.get

  private def spawnContainer(service: BroccoliTestService): Try[String @@ ContainerHandle] = Try {
    val run = Seq(
      "sudo", // Docker requires root.
      "docker",
      "run",
      "--rm", // Automatically remove the container when its stopped
      "-d", // Detach into background
      "--net",
      "host", // Use host network to interconnect all services
      "frosner/cluster-broccoli-test"
    ) ++ service.command
    tag[ContainerHandle](run.!!.trim)
  }

  private def stopContainer(handle: String @@ ContainerHandle): Try[Unit] =
    Try {
      Seq("sudo", "docker", "stop", handle).!!
      ()
    }
}

object BroccoliDockerContext {

  /**
    * Configuration for the Broccoli Docker Context.
    *
    * @param services Services to start
    * @param startupPatience Time to wait after container startup
    */
  final case class Configuration(services: Set[BroccoliTestService], startupPatience: FiniteDuration)

  object Configuration {

    /**
      * Create a configuration to start the given services.
      *
      * @param services The services to start
      * @return The corresponding configuration
      */
    def services(service: BroccoliTestService, services: BroccoliTestService*): Configuration =
      Configuration(services.toSet + service, 10.seconds)
  }
}
