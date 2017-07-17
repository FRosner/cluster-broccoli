package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.IncomingWsMessageType._
import de.frosner.broccoli.models._
import de.frosner.broccoli.models.InstanceUpdate.{instanceUpdateReads, instanceUpdateWrites}
import de.frosner.broccoli.controllers.IncomingWsMessageType.IncomingWsMessageType
import play.api.libs.json._

case class IncomingWsMessage(messageType: IncomingWsMessageType, payload: Object)

object IncomingWsMessage {

  implicit val incomingWsMessageReads: Reads[IncomingWsMessage] = Reads { message =>
    val messageJson = message.as[JsObject]
    val messageType = messageJson.value("messageType").as[IncomingWsMessageType]
    val payloadJson = messageJson.value("payload")
    val payloadObject: JsResult[Object] = messageType match {
      case AddInstance    => Json.fromJson(payloadJson)(InstanceCreation.instanceCreationReads)
      case DeleteInstance => payloadJson.validate[String]
      case UpdateInstance => Json.fromJson(payloadJson)(InstanceUpdate.instanceUpdateReads)
    }
    payloadObject.map(o => IncomingWsMessage(messageType, payloadObject.get))
  }

}
