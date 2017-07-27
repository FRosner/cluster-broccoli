package de.frosner.broccoli.signal

import sun.misc.{Signal, SignalHandler}

/**
  * Provide a way to register and unregister OS signals
  */
trait SignalManager {

  /**
    * Registers signal handler for the given signal
    * @param signal
    * @param handler
    * @throws IllegalArgumentException if a signal is already registered or reserved by JDK or OS
    * @throws UnsupportedOperationException if OS is not supported
    */
  def register(signal: Signal, handler: SignalHandler): Unit

  /**
    * Unregisters the signal
    * @param signal
    */
  def unregister(signal: Signal): Unit
}
