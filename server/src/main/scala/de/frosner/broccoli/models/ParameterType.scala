package de.frosner.broccoli.models

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.StaticSetProvider._
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import scala.collection.immutable
import scala.language.higherKinds

sealed trait ParameterType extends EnumEntry with EnumEntry.Lowercase

object ParameterType extends Enum[ParameterType] with PlayJsonEnum[ParameterType] {
  val values: immutable.IndexedSeq[ParameterType] = findValues
  case object Raw extends ParameterType
  case object String extends ParameterType
  case object Integer extends ParameterType
  case object Decimal extends ParameterType
  case class Set[T](setProvider: SetProvider[T]) extends ParameterType

  implicit val parameterTypeWrites= new Writes[ParameterType] {
    override def writes(parameterType: ParameterType): JsValue = {

      Json.obj(
        "name" ->
          (
            parameterType match {
              case Raw => "raw"
              case String => "string"
              case Integer => "integer"
              case Decimal => "decimal"
              case Set(_) => "set"
              case _ => "unknown"
            }
          ),
        "metadata" ->
          (
            parameterType match {
              case Set(provider) => Some("")
              case _ => None
            }
          )
      )
    }
  }
}

trait SetProvider[T] {
  def getValues: Set[T]
}

object StaticSetProvider {
  def fromJson[T](json: JsValue): StaticSetProvider[T] = {
    StaticSetProvider((json \ "values").as[List[T]].toSet)
  }

  implicit case class StaticIntSetProvider(override val values: Set[Int]) extends StaticSetProvider[Int](values)
  implicit case class StaticDoubleSetProvider(override val values: Set[Double]) extends StaticSetProvider[Double](values)
  implicit case class StaticStringSetProvider(override val values: Set[String]) extends StaticSetProvider[String](values)
}
case class StaticSetProvider[T](values: Set[T]) extends SetProvider[T] {
  override def getValues: Set[T] = values
}

case class UserOESetProvider(account: Account) extends SetProvider[String] {
  override def getValues: Set[String] = Seq(account.getOEPrefix).toSet
}
