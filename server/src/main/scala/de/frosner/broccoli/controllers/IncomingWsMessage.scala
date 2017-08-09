package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import play.api.libs.json._
import enumeratum._

import scala.collection.immutable

/**
  * An incoming message on Broccoli's web socket connection.
  */
sealed trait IncomingWsMessage

object IncomingWsMessage {

  /**
    * The type of an incoming message on the web socket.
    *
    * Entry names are uncaptialised, ie, start with a lowercase letter, for compatibility with the previous Scala Enum
    * declaration and thus the webui frontend.
    */
  sealed trait Type extends EnumEntry with EnumEntry.Uncapitalised

  object Type extends Enum[Type] with PlayJsonEnum[Type] {
    override val values: immutable.IndexedSeq[Type] = findValues

    case object AddInstance extends Type
    case object DeleteInstance extends Type
    case object UpdateInstance extends Type
    case object GetInstanceTasks extends Type
  }

  /**
    * Add a new instance.
    *
    * @param instance A description of the instance to add.
    */
  final case class AddInstance(instance: InstanceCreation) extends IncomingWsMessage

  /**
    * Delete an instance.
    *
    * @param instance The name of the instance to delete
    */
  final case class DeleteInstance(instance: String) extends IncomingWsMessage

  /**
    * Update an instance.
    *
    * @param instance A description of the instance to update
    */
  final case class UpdateInstance(instance: InstanceUpdate) extends IncomingWsMessage

  /**
    * Query the tasks of an instance.
    *
    * @param instance The ID of the instance
    */
  final case class GetInstanceTasks(instance: String) extends IncomingWsMessage

  /**
    * JSON formats for a message incoming from a websocket.
    *
    * The JSON structure is not particularly straight-forward and deviates from what a generated Reads instance would
    * deserialize.  However, it maintains compatibility with the earlier implementation of IncomingWsMessage that used
    * a dedicated "type" enum and an unsafe object-typed payload.
    */
  implicit val incomingWsMessageFormat: Format[IncomingWsMessage] = Format.apply(
    (JsPath \ "messageType").read[Type].flatMap(readsPayload),
    Writes {
      case AddInstance(create)        => write(Type.AddInstance, create)
      case DeleteInstance(instance)   => write(Type.DeleteInstance, instance)
      case UpdateInstance(update)     => write(Type.UpdateInstance, update)
      case GetInstanceTasks(instance) => write(Type.GetInstanceTasks, instance)
    }
  )

  private def readsPayload(`type`: Type): Reads[IncomingWsMessage] = {
    val payload = JsPath \ "payload"
    `type` match {
      case Type.AddInstance      => payload.read[InstanceCreation].map(AddInstance)
      case Type.DeleteInstance   => payload.read[String].map(DeleteInstance)
      case Type.UpdateInstance   => payload.read[InstanceUpdate].map(UpdateInstance)
      case Type.GetInstanceTasks => payload.read[String].map(GetInstanceTasks)
    }
  }

  private def write[P](`type`: Type, payload: P)(implicit writesP: Writes[P]): JsObject =
    Json.obj("messageType" -> `type`, "payload" -> payload)
}
