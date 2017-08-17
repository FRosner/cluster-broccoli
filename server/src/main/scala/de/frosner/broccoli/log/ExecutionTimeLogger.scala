package de.frosner.broccoli.log

import java.util.Date

import play.api.Logger

class ExecutionTimeLogger(logger: Logger) {

  private def logMessage(operation: String, ms: Long) = s"$operation took $ms ms"

  def info[T](operation: String)(f: => T): T = {
    val (result, time) = withExecutionTime(f)
    logger.info(logMessage(operation, time))
    result
  }

  def debug[T](operation: String)(f: => T): T = {
    val (result, time) = withExecutionTime(f)
    logger.debug(logMessage(operation, time))
    result
  }

  def withExecutionTime[T](f: => T): (T, Long) = {
    val start = new Date()
    val result = f
    val end = new Date()
    (result, end.getTime - start.getTime)
  }
}

object ExecutionTimeLogger {
  def apply(log: Logger) = new ExecutionTimeLogger(log)
}
