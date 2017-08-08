package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import de.frosner.broccoli.models.Template.templateApiWrites
import enumeratum.EnumEntry.Camelcase
import play.api.libs.json._
import enumeratum._

import scala.collection.immutable

sealed trait OutgoingWsMessage

object OutgoingWsMessage {

  /**
    * The type of an outgoing message on the web socket.
    *
    * Entry names are uncaptialised, ie, start with a lowercase letter, for compatibility with the previous Scala Enum
    * declaration and thus the webui frontend.
    */
  sealed trait Type extends EnumEntry with EnumEntry.Uncapitalised

  object Type extends Enum[Type] with PlayJsonEnum[Type] {
    override val values: immutable.IndexedSeq[Type] = findValues

    case object ListTemplates extends Type
    case object ListInstances extends Type
    case object AboutInfo extends Type
    case object Error extends Type
    case object Notification extends Type
    case object AddInstanceSuccess extends Type
    case object AddInstanceError extends Type
    case object DeleteInstanceSuccess extends Type
    case object DeleteInstanceError extends Type
    case object UpdateInstanceSuccess extends Type
    case object UpdateInstanceError extends Type
  }

  final case class ListTemplates(templates: Seq[Template]) extends OutgoingWsMessage
  final case class ListInstances(instances: Seq[InstanceWithStatus]) extends OutgoingWsMessage
  final case class AboutInfoMsg(info: AboutInfo) extends OutgoingWsMessage
  final case class Error(error: String) extends OutgoingWsMessage
  final case class Notification(message: String) extends OutgoingWsMessage
  final case class AddInstanceSuccess(result: InstanceCreationSuccess) extends OutgoingWsMessage
  final case class AddInstanceError(result: InstanceCreationFailure) extends OutgoingWsMessage
  final case class DeleteInstanceSuccess(result: InstanceDeleted) extends OutgoingWsMessage
  final case class DeleteInstanceError(error: InstanceError) extends OutgoingWsMessage
  final case class UpdateInstanceSuccess(result: InstanceUpdateSuccess) extends OutgoingWsMessage
  final case class UpdateInstanceError(result: InstanceUpdateFailure) extends OutgoingWsMessage

  def fromResult(result: InstanceCreationResult): OutgoingWsMessage = result match {
    case create: InstanceCreationSuccess => AddInstanceSuccess(create)
    case error: InstanceCreationFailure  => AddInstanceError(error)
  }

  def fromResult(result: InstanceUpdateResult): OutgoingWsMessage = result match {
    case create: InstanceUpdateSuccess => UpdateInstanceSuccess(create)
    case error: InstanceUpdateFailure  => UpdateInstanceError(error)
  }

  /**
    * JSON writes for a message outgoing to a websocket.
    *
    * The JSON structure is not particularly straight-forward and deviates from what a generated Reads instance would
    * deserialize.  However, it maintains compatibility with the earlier implementation of OutgoingWsMessage that used
    * a dedicated "type" enum and an unsafe any-typed payload.
    */
  implicit val outgoingWsMessageWrites: Writes[OutgoingWsMessage] =
    Writes {
      case ListTemplates(templates)      => write(Type.ListTemplates, templates)
      case ListInstances(instances)      => write(Type.ListInstances, instances)
      case AboutInfoMsg(info)            => write(Type.AboutInfo, info)
      case Error(error)                  => write(Type.Error, error)
      case Notification(message)         => write(Type.Notification, message)
      case AddInstanceSuccess(result)    => write(Type.AddInstanceSuccess, result)
      case AddInstanceError(result)      => write(Type.AddInstanceError, result)
      case DeleteInstanceSuccess(result) => write(Type.DeleteInstanceSuccess, result)
      case DeleteInstanceError(result)   => write(Type.DeleteInstanceError, result)
      case UpdateInstanceSuccess(result) => write(Type.UpdateInstanceSuccess, result)
      case UpdateInstanceError(result)   => write(Type.UpdateInstanceError, result)
    }

  private def write[P](`type`: Type, payload: P)(implicit writesP: Writes[P]): JsObject =
    Json.obj("messageType" -> `type`, "payload" -> payload)
}
