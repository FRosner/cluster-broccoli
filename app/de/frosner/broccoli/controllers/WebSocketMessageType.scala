package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.WebSocketMessageType.WebSocketMessageType
import play.api.libs.json.{JsString, Reads, Writes}

object WebSocketMessageType extends Enumeration {

  type WebSocketMessageType = Value

  val ListTemplatesMsg = Value("listTemplates")
  val ListInstancesMsg = Value("listInstances")
  val AboutInfoMsg = Value("aboutInfo")
  //  val AddInstance = Value("addInstance")
  //  val DeleteInstance = Value("deleteInstance")
  //  val UpdateInstance = Value("updateInstance")
  //  val SetInstanceStatuses = Value("setInstanceStatuses")
  //  val SetServiceStatuses = Value("setServiceStatuses")
//  val SetClusterManagerStatus = Value("setClusterManagerStatus")
//  val SetServiceDiscoveryStatus = Value("setServiceDiscoveryStatus")

  implicit val webSocketMessageTypeWrites: Writes[WebSocketMessageType] = Writes(value => JsString(value.toString))

  implicit val webSocketMessageTypeReads: Reads[WebSocketMessageType] = Reads(_.validate[String].map(WebSocketMessageType.withName))

}