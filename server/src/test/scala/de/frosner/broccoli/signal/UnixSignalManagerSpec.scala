package de.frosner.broccoli.signal

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import sun.misc.{Signal, SignalHandler}

class UnixSignalManagerSpec extends Specification with Mockito {
  "Registering new signal" should {
    "fail if the signal is reserved by the JVM" in {
      val manager = new UnixSignalManager()
      manager.register(new Signal("USR1"), mock[SignalHandler]) must throwA(
        new IllegalArgumentException("Signal already used by VM or OS: SIGUSR1"))
    }

    "fail if the signal is already registered" in {
      val manager = new UnixSignalManager()
      val handler = mock[SignalHandler]
      manager.register(new Signal("USR2"), handler)
      manager.register(new Signal("USR2"), handler) must throwA(
        new IllegalArgumentException(s"Signal ${new Signal("USR2")} is already registered"))
    }
  }
}
