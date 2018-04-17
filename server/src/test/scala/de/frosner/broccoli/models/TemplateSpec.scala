package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification
import play.api.libs.json.Json

import Template.{templateApiWrites, templatePersistenceReads}

class TemplateSpec extends Specification {

  "A template" should {

    "extract a single parameter from a template correctly" in {
      Template("test", "Hallo {{id}}", "desc", Map.empty).parameters === Set("id")
    }

    "extract multiple parameters from a template correclty" in {
      Template("test", "Hallo {{id}}, how is {{object}}", "desc", Map.empty).parameters === Set("id", "object")
    }

    "extract a single parameter with multiple occurances from a template correctly" in {
      Template("test", "Hallo {{id}}. I like {{id}}.", "desc", Map.empty).parameters === Set("id")
    }

    "support dashes in the parameter names" in {
      Template("test", "Hallo {{id}} {{person-name}}", "desc", Map.empty).parameters === Set("id", "person-name")
    }

    "support underscores in the parameter names" in {
      Template("test", "Hallo {{id}} {{person_name}}", "desc", Map.empty).parameters === Set("id", "person_name")
    }

    "require an 'id' parameter (no parameter)" in {
      Template("test", "Hallo", "desc", Map.empty).parameters must throwA[IllegalArgumentException]
    }

    "require an 'id' parameter (wrong parameter)" in {
      Template("test", "Hallo {{bla}}", "desc", Map.empty).parameters must throwA[IllegalArgumentException]
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
          "id" -> ParameterInfo("id", None, None, secret = Some(false), `type` = ParameterType.String, orderIndex = None)
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
