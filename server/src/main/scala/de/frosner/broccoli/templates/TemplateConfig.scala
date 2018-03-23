package de.frosner.broccoli.templates

import com.typesafe.config.{ConfigObject, ConfigValue, ConfigValueType}
import de.frosner.broccoli.models.{ParameterInfo, ParameterType, ParameterValue}
import de.frosner.broccoli.services.ParameterValueParsingException
import pureconfig.ConfigReader
import pureconfig.error.{ConfigReaderFailure, ConfigReaderFailures, ConfigValueLocation, ThrowableFailure}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object TemplateConfig {

  implicit val configreader = new ConfigReader[TemplateInfo] {
    override def from(configOrig: ConfigValue): Either[ConfigReaderFailures, TemplateInfo] = {
      Try {
        val confObj = configOrig.asInstanceOf[ConfigObject]
        val config = confObj.toConfig
        val description = Try(config.getString("description")).toOption
        val parameters = Try(config.getConfig("parameters")).map {
          paramsConfig =>
            val ret: Map[String, Parameter] = {
              for {
                entry <- paramsConfig.entrySet().asScala
                paramName = entry.getKey
                paramValueObj <- Try(entry.getValue.asInstanceOf[ConfigObject].toConfig).toOption
                maybeName = Try(paramValueObj.getString("name")).toOption
                maybeSecret = Try(paramValueObj.getBoolean("secret")).toOption
                // Don't wrap the last call to withName as we want it to fail in case the wrong type is supplied
                maybeParamType = Try(paramValueObj.getString("type")).toOption.map(ParameterType.withName)
                maybeOrderIndex = Try(paramValueObj.getInt("orderIndex")).toOption
                maybeDefault = Try(paramValueObj.getValue("default")).toOption.map { paramValueConf =>
                  ParameterValue.constructParameterValueFromTypesafeConfig(
                    maybeParamType.getOrElse(ParameterType.Raw), paramValueConf
                  ) match {
                    case Success(paramDefault) => paramDefault
                    case Failure(ex) => throw ex
                  }
                }
              } yield (paramName, Parameter(maybeName, maybeDefault, maybeSecret, maybeParamType, maybeOrderIndex))
            }.toMap
            ret
        }.toOption
        TemplateInfo(description, parameters)
      } match {
        case Success(e) => Right(e)
        case Failure(ex) =>
          // TODO: Improve this error throwing.
          Left(ConfigReaderFailures(ThrowableFailure(ex, None, "")))
      }
    }
  }



  final case class TemplateInfo(description: Option[String], parameters: Option[Map[String, Parameter]])

  final case class Parameter(name: Option[String],
                             default: Option[ParameterValue],
                             secret: Option[Boolean],
                             `type`: Option[ParameterType],
                             orderIndex: Option[Int])

}
