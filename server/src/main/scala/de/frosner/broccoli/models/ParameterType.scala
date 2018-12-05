package de.frosner.broccoli.models

import com.typesafe.config.{ConfigList, ConfigObject, ConfigValue, ConfigValueType}
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.SetProvider.{
  StaticDoubleSetProvider,
  StaticIntSetProvider,
  StaticStringSetProvider,
  UserOESetProvider
}
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.immutable
import scala.language.implicitConversions

sealed trait ParameterType extends EnumEntry with EnumEntry.Lowercase

object ParameterType extends Enum[ParameterType] with PlayJsonEnum[ParameterType] {
  private val log = play.api.Logger(getClass)

  val values: immutable.IndexedSeq[ParameterType] = findValues
  case object Raw extends ParameterType
  case object String extends ParameterType
  case object Integer extends ParameterType
  case object Decimal extends ParameterType
  case class Set(setProvider: SetProvider) extends ParameterType {

    def getValues(account: Account): JsValue =
      setProvider match {
        case StaticIntSetProvider(set)    => Json.toJson(getValuesInternal(set))
        case StaticDoubleSetProvider(set) => Json.toJson(getValuesInternal(set))
        case StaticStringSetProvider(set) => Json.toJson(getValuesInternal(set))
        case UserOESetProvider            => Json.toJson(getValuesInternal(account))
      }

    private def getValuesInternal(magnet: SetProviderMagnet): magnet.Result = magnet()
  }

  def paramTypeFromString(paramName: String): Option[ParameterType] =
    paramName match {
      case "raw"     => Some(ParameterType.Raw)
      case "string"  => Some(ParameterType.String)
      case "integer" => Some(ParameterType.Integer)
      case "decimal" => Some(ParameterType.Decimal)
      case _         => None
    }

  def fromConfigObject(configValue: ConfigValue): Option[ParameterType] =
    try {
      configValue.valueType() match {
        case ConfigValueType.STRING =>
          paramTypeFromString(configValue.unwrapped().asInstanceOf[String])
        case ConfigValueType.OBJECT =>
          val config = configValue.asInstanceOf[ConfigObject].toConfig
          config.getString("name") match {
            case "raw"     => Some(ParameterType.Raw)
            case "string"  => Some(ParameterType.String)
            case "integer" => Some(ParameterType.Integer)
            case "decimal" => Some(ParameterType.Decimal)
            case "set" =>
              val metadata = config.getObject("metadata")
              metadata.toConfig.getString("provider") match {
                case "StaticIntSetProvider" =>
                  Some(
                    Set(
                      StaticIntSetProvider(
                        metadata.get("values").asInstanceOf[ConfigList].unwrapped().asInstanceOf[List[Int]].toSet
                      )
                    )
                  )
                case "StaticDoubleSetProvider" =>
                  Some(
                    Set(
                      StaticDoubleSetProvider(
                        metadata.get("values").asInstanceOf[ConfigList].unwrapped().asInstanceOf[List[Double]].toSet
                      )
                    )
                  )
                case "StaticStringSetProvider" =>
                  Some(
                    Set(
                      StaticStringSetProvider(
                        metadata.get("values").asInstanceOf[ConfigList].unwrapped().asInstanceOf[List[String]].toSet
                      )
                    )
                  )
                case "UserOESetProvider" =>
                  Some(Set(UserOESetProvider))
                case _ =>
                  None
              }
          }
        case _ => None
      }
    } catch {
      case e: Throwable =>
        log.error("Error while getting parameter type from config object: ", e)
        None
    }

  implicit val parameterTypeWrites: Writes[ParameterType] = new Writes[ParameterType] {

    private val writesFormat = (
      (JsPath \ "name").write[String] ~
        (JsPath \ "metadata").writeNullable[JsObject]
      ).tupled

    override def writes(parameterType: ParameterType): JsValue = {
      val metadata = parameterType match {
        case Set(setProvider) =>
          setProvider match {
            case StaticIntSetProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> "StaticIntSetProvider",
                  "values" -> v
                )
              )
            case StaticDoubleSetProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> "StaticDoubleSetProvider",
                  "values" -> v
                )
              )
            case StaticStringSetProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> "StaticStringSetProvider",
                  "values" -> v
                )
              )
            case UserOESetProvider =>
              Some(
                Json.obj(
                  "provider" -> "UserOESetProvider"
                )
              )
            case _ => None
          }
        case _ => None
      }
      val name =
        parameterType match {
          case Raw     => "raw"
          case String  => "string"
          case Integer => "integer"
          case Decimal => "decimal"
          case Set(_)  => "set"
        }
      writesFormat.writes(name, metadata)
    }
  }

  implicit def parameterTypeApiWrites(implicit account: Account): Writes[ParameterType] = new Writes[ParameterType] {

    private val writesFormat = (
      (JsPath \ "name").write[String] ~
        (JsPath \ "values").writeNullable[JsValue]
      ).tupled

    override def writes(parameterType: ParameterType): JsValue = {
      val values =
        parameterType match {
          case s: Set =>
            Some(s.getValues(account))
          case _ => None
        }
      val name =
        parameterType match {
          case Raw     => "raw"
          case String  => "string"
          case Integer => "integer"
          case Decimal => "decimal"
          case Set(provider) =>
            provider match {
              case StaticDoubleSetProvider(_)                     => "DoubleSet"
              case StaticIntSetProvider(_)                        => "IntSet"
              case StaticStringSetProvider(_) | UserOESetProvider => "StringSet"
            }
        }
      writesFormat.writes(name, values)
    }
  }
}

sealed trait SetProvider extends EnumEntry with EnumEntry.Lowercase
object SetProvider extends Enum[SetProvider] with PlayJsonEnum[SetProvider] {
  val values: immutable.IndexedSeq[SetProvider] = findValues
  case class StaticIntSetProvider(values: Set[Int]) extends SetProvider
  case class StaticDoubleSetProvider(values: Set[Double]) extends SetProvider
  case class StaticStringSetProvider(values: Set[String]) extends SetProvider
  case object UserOESetProvider extends SetProvider
}

sealed trait SetProviderMagnet {
  type Result
  def apply(): Result
}

object SetProviderMagnet {
  implicit def fromStaticInt(values: Set[Int]): SetProviderMagnet {
    type Result = Set[Int]
  } =
    new SetProviderMagnet {
      override type Result = Set[Int]
      override def apply(): Result = values
    }

  implicit def fromStaticDouble(values: Set[Double]): SetProviderMagnet {
    type Result = Set[Double]
  } =
    new SetProviderMagnet {
      override type Result = Set[Double]
      override def apply(): Result = values
    }

  implicit def fromStaticString(values: Set[String]): SetProviderMagnet {
    type Result = Set[String]
  } =
    new SetProviderMagnet {
      override type Result = Set[String]
      override def apply(): Result = values
    }

  implicit def fromUserOEProvider(account: Account): SetProviderMagnet {
    type Result = Set[String]
  } =
    new SetProviderMagnet {
      override type Result = Set[String]
      override def apply(): Result = Seq(account.getOEPrefix).toSet
    }
}
