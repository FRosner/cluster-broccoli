package de.frosner.broccoli.signal

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import sun.misc.{Signal, SignalHandler}

class UnixSignalManagerSpec extends Specification with Mockito {
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

    "fail if the signal is reserved by the JVM" in {
      val manager = new UnixSignalManager()
      manager.register(new Signal("USR1"), mock[SignalHandler]) must throwA(
        new IllegalArgumentException("Signal already used by VM or OS: SIGUSR1"))
    }

    "fail if the signal is already registered" in {
      val manager = new UnixSignalManager()
      val signal = new Signal("USR2")
      val handler = mock[SignalHandler]
      manager.register(signal, handler)
      manager.register(signal, handler) must throwA(
        new IllegalArgumentException(s"Signal $signal is already registered"))

    }
  }

  "Unregistering a signal" should {
    "fail if the signal was not registered" in {
      val manager = new UnixSignalManager()
      val signal = new Signal("USR1")
      manager.unregister(signal) must throwA(new IllegalArgumentException(s"Signal $signal is not registered"))
    }
  }
}
