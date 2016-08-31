package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification

class TemplateSpec extends Specification {

  "A template" should {

    "extract a single parameter from a template correctly" in {
      Template("test", "Hallo {{id}}", "desc").parameters === Set("id")
    }

    "extract multiple parameters from a template correclty" in {
      Template("test", "Hallo {{id}}, how is {{object}}", "desc").parameters === Set("id", "object")
    }

    "extract a single parameter with multiple occurances from a template correctly" in {
      Template("test", "Hallo {{id}}. I like {{id}}.", "desc").parameters === Set("id")
    }

    "support dashes in the parameter names" in {
      Template("test", "Hallo {{id}} {{person-name}}", "desc").parameters === Set("id", "person-name")
    }

    "support underscores in the parameter names" in {
      Template("test", "Hallo {{id}} {{person_name}}", "desc").parameters === Set("id", "person_name")
    }

    "require an 'id' parameter (no parameter)" in {
      Template("test", "Hallo", "desc").parameters must throwA[IllegalArgumentException]
    }

    "require an 'id' parameter (wrong parameter)" in {
      Template("test", "Hallo {{bla}}", "desc").parameters must throwA[IllegalArgumentException]
    }

    "create the template version correctly in" in {
      Template("test", "template JSON", "desc").templateVersion === "d81c4d34fb18636e62ee1b9b6a783bd5"
    }

    "result in different template versions if the template differs" in {
      Template("test", "template JSON", "desc").templateVersion !== Template("test", "template JSONs", "desc").templateVersion
    }

  }

  "Template serialization" should {

    "work correctly" in {
      val originalTemplate = Template("test", "Hallo {{name}}", "desc")
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

}
