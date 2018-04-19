package de.frosner.broccoli.templates

import com.typesafe.config.{ConfigObject, ConfigValue}
import de.frosner.broccoli.models.{ParameterType, ParameterValue}
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailures, ThrowableFailure}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object TemplateConfig {

  implicit val configReader: ConfigReader[TemplateInfo] = new ConfigReader[TemplateInfo] {
    override def from(configOrig: ConfigValue): Either[ConfigReaderFailures, TemplateInfo] =
      Try {
        val confObj = configOrig.asInstanceOf[ConfigObject]
        val config = confObj.toConfig
        val description = Try(config.getString("description")).toOption
        val parameters = Try(config.getObject("parameters")).toOption.map { paramsConfig =>
          paramsConfig.map {
            case (paramName, paramConfig) =>
              val paramValueObj = paramConfig.asInstanceOf[ConfigObject].toConfig
              val maybeName = Try(paramValueObj.getString("name")).toOption
              val maybeSecret = Try(paramValueObj.getBoolean("secret")).toOption
              // Don't wrap the call as we want it to fail in case the wrong type or no type is supplied
              val paramType = ParameterType.withName(paramValueObj.getString("type"))
              val maybeOrderIndex = Try(paramValueObj.getInt("order-index")).toOption
              val maybeDefault = Try(paramValueObj.getValue("default")).toOption.map { paramValueConf =>
                ParameterValue.fromConfigValue(
                  paramName,
                  paramType,
                  paramValueConf
                ) match {
                  case Success(paramDefault) => paramDefault
                  case Failure(ex)           => throw ex
                }
              }
              (paramName, Parameter(maybeName, maybeDefault, maybeSecret, paramType, maybeOrderIndex))
          }.toMap
        }
        TemplateInfo(description, parameters)
      } match {
        case Success(e)  => Right(e)
        case Failure(ex) =>
          // TODO: Improve this error throwing.
          Left(ConfigReaderFailures(ThrowableFailure(ex, None, "")))
      }
  }

  final case class TemplateInfo(description: Option[String], parameters: Option[Map[String, Parameter]])

  final case class Parameter(name: Option[String],
                             default: Option[ParameterValue],
                             secret: Option[Boolean],
                             `type`: ParameterType,
                             orderIndex: Option[Int])

}
