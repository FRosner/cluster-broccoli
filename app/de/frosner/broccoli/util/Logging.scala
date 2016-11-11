package de.frosner.broccoli.util

import play.api.Logger

trait Logging {

  val Logger = play.api.Logger(getClass.getSimpleName)

  def newExceptionWithWarning(exception: Throwable): Throwable = {
    Logger.warn(exception.toString)
    exception
  }

}
