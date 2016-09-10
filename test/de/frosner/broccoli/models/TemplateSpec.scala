package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification

class TemplateSpec extends Specification {

  "A template" should {

    "create the template version correctly in" in {
      Template("test", "template JSON", "desc").templateVersion === "d81c4d34fb18636e62ee1b9b6a783bd5"
    }

    "result in different template versions if the template differs" in {
      Template("test", "template JSON", "desc").templateVersion !== Template("test", "template JSONs", "desc").templateVersion
    }

  }

  "Template parameter extraction" should {

    "compute the parameter names correctly" in {
      Template("test", "Hallo {{id}} {{id}} {{name}}", "desc").parameterNames === Set(
        "id",
        "name"
      )
    }

    "extract a single parameter with simple syntax from a template correctly" in {
      Template("test", "{{id}}", "desc").parameters === Seq(
        Parameter("id", 0, 6)
      )
    }

    "extract multiple parameters with simple syntax from a template correclty" in {
      Template("test", "Hallo {{id}}, how is {{object}}", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("object", 21, 31)
      )
    }

    "extract a single parameter with simple syntax with multiple occurances from a template correctly" in {
      Template("test", "Hallo {{id}}. I like {{id}}.", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("id", 21, 27)
      )
    }

    "support dashes in the parameter with simple syntax names" in {
      Template("test", "Hallo {{id}} {{person-name}}", "desc").parameters === Seq(
        Parameter("id", 6, 12), 
        Parameter("person-name", 13, 28)
      )
    }

    "support underscores in the parameter with simple syntax names" in {
      Template("test", "Hallo {{id}} {{person_name}}", "desc").parameters === Seq(
        Parameter("id", 6, 12),
        Parameter("person_name", 13, 28)
      )
    }

    "extract a single parameter with advanced syntax from a template correctly" in {
      Template("test", "Hallo {{name:id}}", "desc").parameters === Seq(
        Parameter("id", 6, 17)
      )
    }

    "extract multiple parameters with advanced syntax from a template correclty" in {
      Template("test", "Hallo {{name:id}}, how is {{name:object}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("object", 26, 41)
      )
    }

    "extract a single parameter with advanced syntax with multiple occurances from a template correctly" in {
      Template("test", "Hallo {{name:id}}. I like {{name:id}}.", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("id", 26, 37)
      )
    }

    "support dashes in the parameter with advanced syntax names" in {
      Template("test", "Hallo {{name:id}} {{name:person-name}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("person-name", 18, 38)
      )
    }

    "support underscores in the parameter with advanced syntax names" in {
      Template("test", "Hallo {{name:id}} {{name:person_name}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("person_name", 18, 38)
      )
    }

    "extract multiple parameters with mixed syntax from a template correclty" in {
      Template("test", "Hallo {{name:id}}, how is {{object}}", "desc").parameters === Seq(
        Parameter("id", 6, 17),
        Parameter("object", 26, 36)
      )
    }

    "extract no parameter spanning multuple lines from a template correctly" in {
      Template("test", s"""Hallo {{id}} {{
bla
}}""", "desc").parameters === Seq(Parameter("id", 6, 12))
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
