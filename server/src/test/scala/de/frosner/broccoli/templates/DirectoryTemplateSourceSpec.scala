package de.frosner.broccoli.templates

import java.nio.file.{FileSystems, Files, Path}

import de.frosner.broccoli.models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.mockito.Mockito._

import scala.io.Source

class DirectoryTemplateSourceSpec extends Specification with TemporaryTemplatesContext with Mockito {
  "Loading templates from a directory" should {

    def templateRenderer: TemplateRenderer = {
      val templateRenderer = mock[TemplateRenderer]
      when(templateRenderer.validateParameterName(anyString)).thenReturn(true)
      templateRenderer
    }

    "fail if the passed directory is not directory" in {
      val directory = FileSystems.getDefault.getPath("not-a-directory")
      Files.exists(directory) must beFalse
      new DirectoryTemplateSource(directory.toString, mock[TemplateRenderer]).loadTemplates() must throwA(
        new IllegalStateException(s"Templates directory ${directory.toAbsolutePath} is not a directory"))
    }

    "parse fully specified templates correctly" in { templatesDirectory: Path =>
      val templates =
        new DirectoryTemplateSource(templatesDirectory.toString, templateRenderer).loadTemplates()
      templates must contain(
        beEqualTo(Template(
          "curl",
          Source.fromFile(templatesDirectory.resolve("curl/template.json").toFile).mkString,
          "A periodic job that sends an HTTP GET request to a specified address every minute.",
          Map(
            "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, Some(0)),
            "URL" -> ParameterInfo("URL",
                                   None,
                                   Some(RawParameterValue("localhost:8000")),
                                   None,
                                   ParameterType.Raw,
                                   Some(1)),
            "enabled" -> ParameterInfo("enabled",
                                       None,
                                       Some(RawParameterValue("true")),
                                       None,
                                       ParameterType.Raw,
                                       Some(2))
          )
        ))).exactly(1)
    }

    "use a default template description if not provided" in { templatesDirectory: Path =>
      val templates =
        new DirectoryTemplateSource(templatesDirectory.toString, templateRenderer).loadTemplates()
      templates.map(_.description) must contain(beEqualTo("curl-without-decription template")).exactly(1)
    }

    "not contain templates that failed to parse" in { templatesDirectory: Path =>
      val templates =
        new DirectoryTemplateSource(templatesDirectory.toString, templateRenderer).loadTemplates()

      templates.map(_.id) must not contain beEqualTo("broken-template")
    }

  }
}
