package de.frosner.broccoli.models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.specs2.mutable.Specification

class TemplateSpec extends Specification {

  "A template" should {

    "extract a single parameter from a template correctly" in {
      Template("test", "Hallo {{name}}", "desc").parameters === Set("name")
    }

    "extract multiple parameters from a template correclty" in {
      Template("test", "Hallo {{name}}, how is {{object}}", "desc").parameters === Set("name", "object")
    }

    "extract a single parameter with multiple occurances from a template correctly" in {
      Template("test", "Hallo {{name}}. I like {{name}}.", "desc").parameters === Set("name")
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
