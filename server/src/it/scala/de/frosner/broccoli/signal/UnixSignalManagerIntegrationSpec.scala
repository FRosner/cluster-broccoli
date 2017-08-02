package de.frosner.broccoli.signal

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import sun.misc.{Signal, SignalHandler}

class UnixSignalManagerIntegrationSpec extends Specification with Mockito {
  "Registering new signal" should {
    "trigger the handler when the signal is raised" in {
      val manager = new UnixSignalManager()
      val signal = new Signal("USR2")
      val handler = mock[SignalHandler]
      manager.register(signal, handler)
      Signal.raise(signal)
      Thread.sleep(1000)
      there was one(handler).handle(signal)
    }
  }
}
