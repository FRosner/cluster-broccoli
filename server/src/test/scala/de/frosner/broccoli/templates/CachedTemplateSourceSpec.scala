package de.frosner.broccoli.templates

import org.specs2.mutable.Specification
import org.mockito.Mockito._
import org.specs2.mock.Mockito

class CachedTemplateSourceSpec extends Specification with Mockito {
  "Loading templates from cache " should {
    "load the templates from the underlying source into cache on the first run" in {
      val testTemplateSource = mock[TemplateSource]
      val cachedTemplateSource = new CachedTemplateSource(testTemplateSource)

      verify(testTemplateSource, times(0)).loadTemplates()
      val templates = cachedTemplateSource.loadTemplates()

      verify(testTemplateSource, times(1)).loadTemplates()
      templates must beEqualTo(testTemplateSource.loadTemplates())
    }

    "return cached results on subsequent runs" in {
      val testTemplateSource = mock[TemplateSource]
      val cachedTemplateSource = new CachedTemplateSource(testTemplateSource)

      val templates1 = cachedTemplateSource.loadTemplates()
      val templates2 = cachedTemplateSource.loadTemplates()

      verify(testTemplateSource, times(1)).loadTemplates()
      templates1 must beEqualTo(templates2)
    }
  }
}
