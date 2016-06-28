package de.frosner.broccoli.models

import org.specs2.mutable.Specification

class TemplateSpec extends Specification {

  "A template" should {

    "extract a single parameter from a template correctly" in {
      Template("test", "Hallo {{name}}", "desc").parameters === Set("name")
    }

    "extract multiple parameters from a template correclty" in {
      Template("test", "Hallo {{name}}, how is {{object}}", "desc").parameters === Set("name", "object")
    }

  }

}
