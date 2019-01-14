package de.frosner.broccoli.models

import com.typesafe.config.{ConfigList, ConfigObject, ConfigValue, ConfigValueType}
import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models.ListProvider.{
  StaticDoubleListProvider,
  StaticIntListProvider,
  StaticStringListProvider,
  UserOEListProvider
}
import de.frosner.broccoli.services.ParameterTypeException
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.{immutable, JavaConverters, SortedSet}
import scala.language.implicitConversions

sealed trait ParameterType extends EnumEntry with EnumEntry.Lowercase

object ParameterType extends Enum[ParameterType] with PlayJsonEnum[ParameterType] {
  private val log = play.api.Logger(getClass)

  val values: immutable.IndexedSeq[ParameterType] = findValues
  case object Raw extends ParameterType
  case object String extends ParameterType
  case object Integer extends ParameterType
  case object Decimal extends ParameterType
  case class List(provider: ListProvider) extends ParameterType {

    def getValues(account: Account): JsValue =
      provider match {
        case StaticIntListProvider(list)    => Json.toJson(getValuesInternal(list))
        case StaticDoubleListProvider(list) => Json.toJson(getValuesInternal(list))
        case StaticStringListProvider(list) => Json.toJson(getValuesInternal(list))
        case UserOEListProvider             => Json.toJson(getValuesInternal(account))
      }

    private def getValuesInternal(magnet: ListProviderMagnet): magnet.Result = magnet()
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
            case "list" =>
              val metadata = config.getObject("metadata")
              metadata.toConfig.getString("provider") match {
                case StaticIntListProvider.name =>
                  Some(
                    List(
                      StaticIntListProvider(
                        JavaConverters
                          .iterableAsScalaIterableConverter(
                            metadata
                              .get("values")
                              .asInstanceOf[ConfigList]
                              .unwrapped()
                              .asInstanceOf[java.util.ArrayList[Int]]
                          )
                          .asScala
                          .toList
                      )
                    )
                  )
                case StaticDoubleListProvider.name =>
                  Some(
                    List(
                      StaticDoubleListProvider(
                        JavaConverters
                          .iterableAsScalaIterableConverter(
                            metadata
                              .get("values")
                              .asInstanceOf[ConfigList]
                              .unwrapped()
                              .asInstanceOf[java.util.ArrayList[Double]]
                          )
                          .asScala
                          .toList
                      )
                    )
                  )
                case StaticStringListProvider.name =>
                  Some(
                    List(
                      StaticStringListProvider(
                        JavaConverters
                          .iterableAsScalaIterableConverter(
                            metadata
                              .get("values")
                              .asInstanceOf[ConfigList]
                              .unwrapped()
                              .asInstanceOf[java.util.ArrayList[String]]
                          )
                          .asScala
                          .toList
                      )
                    )
                  )
                case UserOEListProvider.name =>
                  Some(List(UserOEListProvider))
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

  def fromJson(id: String, json: JsValue): ParameterType =
    json match {
      case JsString(value) =>
        // TODO: This section is for older compatibility. Remove it in the future
        paramTypeFromString(value).getOrElse(throw ParameterTypeException(s"Unsupported data type for parameter $id"))
      case JsObject(underlying) =>
        underlying.getOrElse("name", JsNull) match {
          case JsString(name) =>
            name match {
              case "raw"     => ParameterType.Raw
              case "string"  => ParameterType.String
              case "integer" => ParameterType.Integer
              case "decimal" => ParameterType.String
              case "list" =>
                val metadata =
                  underlying.getOrElse(
                    "metadata",
                    throw ParameterTypeException(s"set type must have metadata in param $id")
                  )
                // TODO: We could use reflection here so user provided implementations also work
                (metadata \ "provider").as[String] match {
                  case "StaticIntSetProvider" =>
                    ParameterType.List(StaticIntListProvider((metadata \ "values").as[collection.immutable.List[Int]]))
                  case "StaticDoubleSetProvider" =>
                    ParameterType.List(
                      StaticDoubleListProvider((metadata \ "values").as[collection.immutable.List[Double]]))
                  case "StaticStringSetProvider" =>
                    ParameterType.List(
                      StaticStringListProvider((metadata \ "values").as[collection.immutable.List[String]]))
                  case "UserOESetProvider" =>
                    ParameterType.List(UserOEListProvider)
                  case _ =>
                    throw ParameterTypeException(s"Unsupported provider class in metadata for parameter `$id`")
                }
            }
          case _ => throw ParameterTypeException(s"Invalid json for key `name` in metadata for parameter `$id`")
        }
      case _ => throw ParameterTypeException(s"Could not parse `type` for parameter `$id`")
    }

  implicit val parameterTypeWrites: Writes[ParameterType] = new Writes[ParameterType] {

    private val writesFormat = (
      (JsPath \ "name").write[String] ~
        (JsPath \ "metadata").writeNullable[JsObject]
    ).tupled

    override def writes(parameterType: ParameterType): JsValue = {
      val metadata = parameterType match {
        case List(listProvider) =>
          listProvider match {
            case StaticIntListProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> StaticIntListProvider.name,
                  "values" -> v
                )
              )
            case StaticDoubleListProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> StaticDoubleListProvider.name,
                  "values" -> v
                )
              )
            case StaticStringListProvider(v) =>
              Some(
                Json.obj(
                  "provider" -> StaticStringListProvider.name,
                  "values" -> v
                )
              )
            case UserOEListProvider =>
              Some(
                Json.obj(
                  "provider" -> UserOEListProvider.name
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
          case List(_) => "list"
        }
      writesFormat.writes((name, metadata))
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
          case s: List =>
            Some(s.getValues(account))
          case _ => None
        }
      val name =
        parameterType match {
          case Raw     => "raw"
          case String  => "string"
          case Integer => "integer"
          case Decimal => "decimal"
          case List(provider) =>
            provider match {
              case StaticDoubleListProvider(_)                      => "decimalList"
              case StaticIntListProvider(_)                         => "intList"
              case StaticStringListProvider(_) | UserOEListProvider => "stringList"
            }
        }
      writesFormat.writes((name, values))
    }
  }
}

sealed trait ListProvider extends EnumEntry with EnumEntry.Lowercase {
  val name: String
}
class StaticListProvider[T](values: List[T]) {
  require(values.nonEmpty, "Value set must be non empty")
}

object ListProvider extends Enum[ListProvider] with PlayJsonEnum[ListProvider] {
  val values: immutable.IndexedSeq[ListProvider] = findValues
  object StaticIntListProvider {
    val name = "StaticIntListProvider"
  }
  case class StaticIntListProvider(values: List[Int]) extends StaticListProvider(values) with ListProvider {
    override val name: String = StaticIntListProvider.name
  }
  object StaticDoubleListProvider {
    val name = "StaticDoubleListProvider"
  }
  case class StaticDoubleListProvider(values: List[Double]) extends StaticListProvider(values) with ListProvider {
    override val name: String = StaticDoubleListProvider.name
  }
  object StaticStringListProvider {
    val name = "StaticStringListProvider"
  }
  case class StaticStringListProvider(values: List[String]) extends StaticListProvider(values) with ListProvider {
    override val name: String = StaticStringListProvider.name
  }
  case object UserOEListProvider extends ListProvider {
    override val name = "UserOEListProvider"
  }
}

sealed trait ListProviderMagnet {
  type Result
  def apply(): Result
}

object ListProviderMagnet {
  implicit def fromStaticInt(values: List[Int]): ListProviderMagnet {
    type Result = List[Int]
  } =
    new ListProviderMagnet {
      override type Result = List[Int]
      override def apply(): Result = values
    }

  implicit def fromStaticDouble(values: List[Double]): ListProviderMagnet {
    type Result = List[Double]
  } =
    new ListProviderMagnet {
      override type Result = List[Double]
      override def apply(): Result = values
    }

  implicit def fromStaticString(values: List[String]): ListProviderMagnet {
    type Result = List[String]
  } =
    new ListProviderMagnet {
      override type Result = List[String]
      override def apply(): Result = values
    }

  implicit def fromUserOEProvider(account: Account): ListProviderMagnet {
    type Result = List[String]
  } =
    new ListProviderMagnet {
      override type Result = List[String]
      override def apply(): Result = Seq(account.getOEPrefix).toList
    }
}
