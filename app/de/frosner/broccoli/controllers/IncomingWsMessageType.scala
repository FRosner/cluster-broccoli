package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.OutgoingWsMessageType.OutgoingWsMessageType
import play.api.libs.json.{JsString, Reads, Writes}

object IncomingWsMessageType extends Enumeration {

  type IncomingWsMessageType = Value

  val AddInstance = Value("addInstance")
  val DeleteInstance = Value("deleteInstance")
  val UpdateInstance = Value("updateInstance")

  implicit val webSocketMessageTypeReads: Reads[IncomingWsMessageType] = Reads(_.validate[String].map(IncomingWsMessageType.withName))

}