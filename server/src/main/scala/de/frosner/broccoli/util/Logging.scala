package de.frosner.broccoli.util

import java.util.Date

trait Logging {

  val Logger = play.api.Logger(getClass.getSimpleName)

  val TimeLogger = ExecutionTimeLogger(Logger)

  def newExceptionWithWarning(exception: Throwable): Throwable = {
    Logger.warn(exception.toString)
    exception
  }

}

case class ExecutionTimeLogger(private val logger: play.api.Logger) {

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
