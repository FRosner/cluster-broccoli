package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import shapeless.tag
import shapeless.tag.@@

/**
  * A partial model of a Nomad node.
  *
  * @param id The Nomad ID (as UUID)
  * @param name The name of the node
  * @param httpAddress The HTTP address of the node, for client API requests
  */
final case class Node(
    id: String @@ Node.Id,
    name: String @@ Node.Name,
    httpAddress: String @@ Node.HttpAddress
)

object Node {
  sealed trait Id
  sealed trait Name
  sealed trait HttpAddress

  implicit val nodeReads: Reads[Node] = (
    (JsPath \ "ID").read[String].map(tag[Id](_)) and
      (JsPath \ "Name").read[String].map(tag[Name](_)) and
      (JsPath \ "HTTPAddr").read[String].map(tag[HttpAddress](_))
  )(Node.apply _)
}
