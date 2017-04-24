package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.OutgoingWsMessageType.OutgoingWsMessageType
import play.api.libs.json.{JsString, Reads, Writes}

object OutgoingWsMessageType extends Enumeration {

  type OutgoingWsMessageType = Value

  val ListTemplatesMsg = Value("listTemplates")
  val ListInstancesMsg = Value("listInstances")
  val AboutInfoMsg = Value("aboutInfo")
  val ErrorMsg = Value("error")

  implicit val webSocketMessageTypeWrites: Writes[OutgoingWsMessageType] = Writes(value => JsString(value.toString))

}