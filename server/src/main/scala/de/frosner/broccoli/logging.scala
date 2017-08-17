package de.frosner.broccoli

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

/**
  * Utilities for Logging
  */
object logging {

  /**
    * Log the time it takes to execute a block.
    *
    * @param label The human-readable label describing the operation the block performs
    * @param block The block of code to measure
    * @param log A function to emit the log message
    * @tparam T The type of the result of the block
    * @return The result of running the block
    */
  def logExecutionTime[T](label: String)(block: => T)(log: (=> String) => Unit): T = {
    val start = System.nanoTime()
    val result = block
    val duration = Duration(System.nanoTime() - start, TimeUnit.NANOSECONDS)
    log(s"$label took ${duration.toMillis} ms")
    result
  }
}
