package de.frosner.broccoli.nomad.models

import de.frosner.broccoli.util.Resources
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class NodeSpec extends Specification {
  "Node" should {
    "decode from JSON" in {
      val node = Json
        .parse(Resources.readAsString("/de/frosner/broccoli/services/nomad/node.json"))
        .validate[Node]
        .asEither

      node should beRight(
        Node(
          id = shapeless.tag[Node.Id]("4beac5b7-3974-3ddf-b572-9db5906fb891"),
          name = shapeless.tag[Node.Name]("vc31"),
          httpAddress = shapeless.tag[Node.HttpAddress]("127.0.0.1:4646")
        ))
    }
  }
}
