package de.frosner.broccoli.util

import play.api.Logger

trait Logging {

  def newExceptionWithWarning(exception: Throwable): Throwable = {
    Logger.warn(exception.toString)
    exception
  }

}
