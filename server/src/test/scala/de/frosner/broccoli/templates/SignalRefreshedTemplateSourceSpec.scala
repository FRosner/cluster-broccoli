package de.frosner.broccoli.templates

import de.frosner.broccoli.signal.SignalManager
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Matchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import sun.misc.{Signal, SignalHandler}

class SignalRefreshedTemplateSourceSpec extends Specification with Mockito {
  "Receiving a SIGUSR2 signal" should {
    "update the cache" in {
      val signalManager = mock[SignalManager]
      val testTemplateSource = mock[CachedTemplateSource]
      val signalRefreshedTemplateSource = new SignalRefreshedTemplateSource(testTemplateSource, signalManager)
      val handler = ArgumentCaptor.forClass(classOf[SignalHandler])
      there was one(signalManager).register(Matchers.eq(new Signal("USR2")), handler.capture())

      there was no(testTemplateSource).refresh()
      there was no(testTemplateSource).loadTemplates()
      signalRefreshedTemplateSource.loadTemplates()

      there was no(testTemplateSource).refresh()
      there was one(testTemplateSource).loadTemplates(false)
      verify(testTemplateSource, times(1)).loadTemplates(false)

      handler.getValue.handle(new Signal("USR2"))
      there was one(testTemplateSource).refresh()
      there was one(testTemplateSource).loadTemplates(false)
    }
  }
}
