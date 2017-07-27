package de.frosner.broccoli.signal

import org.apache.commons.lang3.SystemUtils
import sun.misc.{Signal, SignalHandler}

import scala.collection.mutable

class UnixSignalManager extends SignalManager {
  private val signals = mutable.HashMap.empty[Signal, SignalHandler]

  def register(signal: Signal, handler: SignalHandler): Unit =
    if (SystemUtils.IS_OS_UNIX) {
      if (signals.contains(signal)) {
        throw new IllegalArgumentException(s"Signal $signal is already registered")
      }

      Signal.handle(signal, handler)
      signals.put(signal, handler)
    } else {
      throw new UnsupportedOperationException("Signal handling is only supported on UNIX")
    }

  def unregister(signal: Signal): Unit =
    if (signals.contains(signal)) {
      Signal.handle(signal, new SignalHandler {
        override def handle(signal: Signal): Unit = {}
      })
      signals.remove(signal)
    }
}
