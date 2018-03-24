package de.frosner.broccoli.templates

import com.typesafe.config.ConfigFactory
import de.frosner.broccoli.models.{IntParameterValue, ParameterType, RawParameterValue, StringParameterValue}
import de.frosner.broccoli.templates.TemplateConfig.Parameter
import org.specs2.mutable.Specification
import pureconfig.error.ConfigReaderException
import pureconfig.loadConfigOrThrow

class TemplateConfigSpec extends Specification {

  "TemplateConfigReader " should {

    "be able to parse config correctly" in {
      val configString =
        """
                  |description = "A periodic job that sends an HTTP GET request to a specified address every minute."
                  |
                  |parameters = {
                  |  "URL" = {
                  |    name = "connection url"
                  |    default = "localhost:8000"
                  |    type = string
                  |    order-index = 1
                  |  }
                  |  "enabled" = {
                  |    default = true
                  |    type = raw
                  |    order-index = 2
                  |  }
                  |  "id" = {
                  |    type = string
                  |  }
                  |}
                """.stripMargin
      val expectedTemplateInfo = TemplateConfig.TemplateInfo(
        description = Some("A periodic job that sends an HTTP GET request to a specified address every minute."),
        parameters = Some(
          Map(
            "URL" -> Parameter(
              name = Some("connection url"),
              default = Some(StringParameterValue("localhost:8000")),
              secret = None,
              `type` = Some(ParameterType.String),
              orderIndex = Some(1)
            ),
            "enabled" -> Parameter(
              name = None,
              default = Some(RawParameterValue("true")),
              secret = None,
              `type` = Some(ParameterType.Raw),
              orderIndex = Some(2)
            ),
            "id" -> Parameter(
              name = None,
              default = None,
              secret = None,
              `type` = Some(ParameterType.String),
              orderIndex = None
            )
          )
        )
      )
      import TemplateConfig.configReader
      val parsedTemplateInfo = loadConfigOrThrow[TemplateConfig.TemplateInfo](
        ConfigFactory.parseString(configString)
      )
      parsedTemplateInfo mustEqual expectedTemplateInfo
    }

    "fail if the template has an integer and tries to parse it as a string" in {

      val configString =
        """
                  |description = "A periodic job that sends an HTTP GET request to a specified address every minute."
                  |
                  |parameters = {
                  |  "URL" = {
                  |    name = "connection url"
                  |    default = "localhost:8000"
                  |    type = string
                  |    order-index = 1
                  |  }
                  |  "count" = {
                  |    default = 12
                  |    type = string
                  |    order-index = 2
                  |  }
                  |  "id" = {
                  |    type = string
                  |  }
                  |}
                """.stripMargin
      loadConfigOrThrow[TemplateConfig.TemplateInfo](
        ConfigFactory.parseString(configString)
      ) must throwA[ConfigReaderException[TemplateConfig.TemplateInfo]]
    }
  }
}
