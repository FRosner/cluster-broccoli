package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.OutgoingWsMessageType.OutgoingWsMessageType
import play.api.libs.json.{JsString, Reads, Writes}

object IncomingWsMessageType extends Enumeration {

  type IncomingWsMessageType = Value

  val AddInstance = Value("addInstance")
  //  val DeleteInstance = Value("deleteInstance")
  //  val UpdateInstance = Value("updateInstance")
  //  val SetInstanceStatuses = Value("setInstanceStatuses")
  //  val SetServiceStatuses = Value("setServiceStatuses")
//  val SetClusterManagerStatus = Value("setClusterManagerStatus")
//  val SetServiceDiscoveryStatus = Value("setServiceDiscoveryStatus")

  implicit val webSocketMessageTypeReads: Reads[IncomingWsMessageType] = Reads(_.validate[String].map(IncomingWsMessageType.withName))

}