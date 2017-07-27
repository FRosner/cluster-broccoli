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
    */
  def register(signal: Signal, handler: SignalHandler): Unit

  /**
    * Unregisters the signal
    * @param signal
    */
  def unregister(signal: Signal): Unit
}
