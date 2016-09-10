package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification

class TemplateSpec extends Specification {

  "A template version" should {

    "get created correctly in" in {
      Template("test", "template JSON", "desc").templateVersion === "d81c4d34fb18636e62ee1b9b6a783bd5"
    }

    "be different for different templates" in {
      Template("test", "template JSON", "desc").templateVersion !== Template("test", "template JSONs", "desc").templateVersion
    }

  }

  "Template parameter names" should {

    "get extracted correctly" in {
      Template("test", "Hallo {{id}} {{id}} {{name}}", "desc").parameterNames === Set(
        "id",
        "name"
      )
    }

  }

  "Parameter name extraction (simple syntax)" should {

    "extract a single parameter correctly" in {
      Template("test", "{{id}}", "desc").parameters === Seq(
        Parameter("id", 0, 6)
      )
    }

    "extract multiple parameters correctly" in {
      Template("test", "Hallo {{id}}, how is {{object}}", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("object", 21, 31)
      )
    }

    "extract a single parameter with multiple occurances" in {
      Template("test", "Hallo {{id}}. I like {{id}}.", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("id", 21, 27)
      )
    }

    "support dashes in the parameter name" in {
      Template("test", "Hallo {{id}} {{person-name}}", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("person-name", 13, 28)
      )
    }

    "support underscores in the parameter name" in {
      Template("test", "Hallo {{id}} {{person_name}}", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("person_name", 13, 28)
      )
    }

  }

  "Parameter name extraction (advanced syntax)" should {

    "extract a single parameter correctly" in {
      Template("test", "Hallo {{name:id}}", "desc").parameters === Seq(
        Parameter("id", 6, 17)
      )
    }

    "extract multiple parameters correctly" in {
      Template("test", "Hallo {{name:id}}, how is {{name:object}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("object", 26, 41)
      )
    }

    "extract a single parameter with multiple occurances correctly" in {
      Template("test", "Hallo {{name:id}}. I like {{name:id}}.", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("id", 26, 37)
      )
    }

    "support dashes in the parameter names" in {
      Template("test", "Hallo {{name:id}} {{name:person-name}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("person-name", 18, 38)
      )
    }

    "support underscores in the parameter names" in {
      Template("test", "Hallo {{name:id}} {{name:person_name}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("person_name", 18, 38)
      )
    }

    "not extract a parameter with an invalid name key" in {
      Template("test", s"{{id}} {{nname:bla}}", "desc").parameters === Seq(
        Parameter("id", 0, 6)
      )
    }

    "not extract a parameter with an invalid name value" in {
      Template("test", s"{{id}} {{name:23}}", "desc").parameters === Seq(
        Parameter("id", 0, 6)
      )
    }

  }

  "Parameter extraction (misc)" should {

    "extract multiple parameters with mixed syntax correctly" in {
      Template("test", "Hallo {{name:id}}, how is {{object}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("object", 26, 36)
      )
    }

    "extract no parameter spanning multiple lines correctly" in {
      Template("test", s"""Hallo {{id}} {{
bla
}}""", "desc").parameters === Seq(Parameter("id", 6, 12))
    }

    "require an 'id' parameter (no parameter)" in {
      Template("test", "Hallo", "desc").parameters must throwA[IllegalArgumentException]
    }

    "require an 'id' parameter (wrong parameter)" in {
      Template("test", "Hallo {{bla}}", "desc").parameters must throwA[IllegalArgumentException]
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
