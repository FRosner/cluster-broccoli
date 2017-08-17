package de.frosner.broccoli.log

import org.specs2.mutable.Specification

import play.api.Logger

class LoggingSpec extends Specification {

  "An ExecutionTimeLogger" should {

    s"not execute twice" in {
      val logger = ExecutionTimeLogger(Logger(getClass))
      var timesExecuted = 0
      logger.withExecutionTime {
        timesExecuted += 1
      }
      timesExecuted === 1
    }

  }

}
