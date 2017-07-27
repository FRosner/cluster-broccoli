package de.frosner.broccoli.templates

import org.mockito.Mockito.{times, verify}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import sun.misc.Signal

class SignalRefreshedTemplateSourceSpec extends Specification with Mockito {
  "Receiving a SIGUSR2 signal" should {
    "update the cache" in {
      val testTemplateSource = mock[CachedTemplateSource]
      val signalRefreshedTemplateSource = new SignalRefreshedTemplateSource(testTemplateSource)

      there was no(testTemplateSource).refresh()
      there was no(testTemplateSource).loadTemplates()
      signalRefreshedTemplateSource.loadTemplates()

      there was no(testTemplateSource).refresh()
      there was one(testTemplateSource).loadTemplates()
      verify(testTemplateSource, times(1)).loadTemplates()

      Signal.raise(new Signal("USR2"))
      Thread.sleep(1000) // wait till the signal was delivered
      there was one(testTemplateSource).refresh()
      there was one(testTemplateSource).loadTemplates()
    }
  }
}
