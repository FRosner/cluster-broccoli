package de.frosner.broccoli.controllers

import de.frosner.broccoli.controllers.WebSocketMessageType._
import de.frosner.broccoli.models.{AboutInfo, InstanceWithStatus, Template}
import Template.templateApiWrites
import InstanceWithStatus.instanceWithStatusWrites
import play.api.libs.json._

case class WebSocketMessage(messageType: WebSocketMessageType, payload: Object)

object WebSocketMessage {

  implicit val webSocketMessageWrites: Writes[WebSocketMessage] = Writes {
    case WebSocketMessage(ListTemplatesMsg, payload: Seq[Template]) =>
      wrap(ListTemplatesMsg, Json.toJson(payload))
    case WebSocketMessage(ListInstancesMsg, payload: Seq[InstanceWithStatus]) =>
      wrap(ListInstancesMsg, Json.toJson(payload))
    case WebSocketMessage(AboutInfoMsg, payload: AboutInfo) =>
      wrap(AboutInfoMsg, Json.toJson(payload))
    case WebSocketMessage(ErrorMsg, payload: String) =>
      wrap(ErrorMsg, Json.toJson(payload))
  }

  implicit val webSocketMessageReads: Reads[WebSocketMessage] = Reads { message =>
    val messageJson = message.as[JsObject]
    val messageType = messageJson.value("messageType").as[WebSocketMessageType]
    val payloadJson = messageJson.value("payload")
    val payloadObject: JsResult[Object] = messageType match {
      // Json.fromJson[Seq[Template]](payloadJson)
      case ListTemplatesMsg => throw new UnsupportedOperationException() // TODO proper error message
      case ListInstancesMsg => throw new UnsupportedOperationException() // TODO proper error message
      case AboutInfoMsg => throw new UnsupportedOperationException() // TODO proper error message
      case ErrorMsg => throw new UnsupportedOperationException() // TODO proper error message
    }
    payloadObject.map(o => WebSocketMessage(messageType, payloadObject))
  }

  private def wrap(messageType: WebSocketMessageType, payload: JsValue): JsValue = JsObject(Map(
    "messageType" -> Json.toJson(messageType),
    "payload" -> payload
  ))

}
