package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.IncomingWsMessageType._
import de.frosner.broccoli.models.{AboutInfo, InstanceCreation, InstanceWithStatus, Template}
import de.frosner.broccoli.controllers.IncomingWsMessageType.IncomingWsMessageType
import play.api.libs.json._

case class IncomingWsMessage(messageType: IncomingWsMessageType, payload: Object)

object IncomingWsMessage {

  implicit val incomingWsMessageReads: Reads[IncomingWsMessage] = Reads { message =>
    val messageJson = message.as[JsObject]
    val messageType = messageJson.value("messageType").as[IncomingWsMessageType]
    val payloadJson = messageJson.value("payload")
    val payloadObject: JsResult[Object] = messageType match {
      case AddInstance => Json.fromJson(payloadJson)(InstanceCreation.instanceCreationReads)
    }
    payloadObject.map(o => IncomingWsMessage(messageType, payloadObject.get))
  }

}
