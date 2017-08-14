package de.frosner.broccoli.models

import de.frosner.broccoli.http.ToHTTPResult.ToToHTTPResultOps
import de.frosner.broccoli.nomad
import org.specs2.ScalaCheck
import play.api.libs.json.Json
import play.api.test.PlaySpecification

import scala.concurrent.Future

class InstanceTasksSpec
    extends PlaySpecification
    with ScalaCheck
    with ModelArbitraries
    with nomad.ModelArbitraries
    with ToToHTTPResultOps {
  "The ToHTTPResult instance" should {
    "convert in 200 OK result" in prop { (instanceTasks: InstanceTasks) =>
      status(Future.successful(instanceTasks.toHTTPResult)) === OK
    }

    "convert the tasks to a JSON body" in prop { (instanceTasks: InstanceTasks) =>
      contentAsJson(Future.successful(instanceTasks.toHTTPResult)) === Json.toJson(instanceTasks.tasks)
    }
  }
}
