package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification
import play.api.libs.json.Json

import Template.{templateApiWrites, templatePersistenceReads}

class TemplateSpec extends Specification {

  "A template" should {

    "extract only parameters specified in the parameters" in {
      Template("test",
               "Hallo {{id}}. I like {{person_name}}.",
               "desc",
               Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None))).parameters === Set("id")
    }

    "not automatically extract parameters from a template" in {
      Template("test", "Hallo {{id}}, how is {{object}}", "desc", Map.empty).parameters === Set.empty
    }

    "create the template version correctly in" in {
      Template("test", "template JSON", "desc", Map.empty).version === "889df4c8118c30a28ed4f51674a0f19d"
    }

    "result in different template versions if the template JSON differs" in {
      Template("test", "template JSON", "desc", Map.empty).version !== Template("test",
                                                                                "template JSONs",
                                                                                "desc",
                                                                                Map.empty).version
    }

    "result in different template versions if the template parameter info differs" in {
      Template(
        id = "test",
        template = "template JSON {{id}}",
        description = "desc",
        parameterInfos = Map.empty
      ).version !== Template(
        id = "test",
        template = "template JSON {{id}}",
        description = "desc",
        parameterInfos = Map(
          "id" -> ParameterInfo("id",
                                None,
                                None,
                                secret = Some(false),
                                `type` = ParameterType.String,
                                orderIndex = None)
        )
      ).version
    }

  }

  "Template serialization" should {

    "work correctly" in {
      val originalTemplate = Template("test", "Hallo {{name}}", "desc", Map.empty)
      val bos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(bos)
      oos.writeObject(originalTemplate)
      oos.close()

      val ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray))
      val deserializedTemplate = ois.readObject()
      ois.close()

      originalTemplate === deserializedTemplate
    }

  }

  "Template back-end JSON serialization" should {

    "work" in {
      val template = Template(
        id = "t",
        template = "{{id}}",
        description = "d",
        parameterInfos = Map.empty
      )
      Json
        .fromJson(Json.toJson(template)(Template.templatePersistenceWrites))(Template.templatePersistenceReads)
        .get === template
    }

  }

}
