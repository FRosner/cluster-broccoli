package de.frosner.broccoli.services

import java.io._

import de.frosner.broccoli.models.Instance
import de.frosner.broccoli.util.Logging
import play.api.Logger
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

trait InstanceStorage extends Logging {

  val prefix: String
  protected def checkPrefix[F, T](id: String)(f: => Try[T]): Try[T] = {
    if (id.startsWith(prefix)) {
      f
    } else {
      Failure(PrefixViolationException(id, prefix))
    }
  }

  private def ifNotClosed[T](f: => T): T = {
    if (!isClosed) {
      f
    } else {
      throw new IllegalStateException(s"$this has already been closed.")
    }
  }

  /**
    * Reads the instance with the specified ID.
    */
  def readInstance(id: String): Try[Instance] = TimeLogger.info(s"readInstance($id)") {
    ifNotClosed {
      readInstanceImpl(id)
    }
  }

  protected def readInstanceImpl(id: String): Try[Instance]



  /**
    * Reads all instances.
    */
  def readInstances(): Try[Set[Instance]] = TimeLogger.info(s"readInstances()") {
    ifNotClosed {
      readInstancesImpl
    }
  }

  protected def readInstancesImpl: Try[Set[Instance]]



  /**
    * Reads all instances whose IDs match the given filter.
    */
  def readInstances(idFilter: String => Boolean): Try[Set[Instance]] = TimeLogger.info(s"readInstances(idFilter)") {
    ifNotClosed {
      readInstancesImpl(idFilter)
    }
  }

  protected def readInstancesImpl(idFilter: String => Boolean): Try[Set[Instance]]

  /**
    * Persists an instance.
    */
  def writeInstance(instance: Instance): Try[Instance] = TimeLogger.info(s"writeInstance(${instance.id})") {
    ifNotClosed {
      writeInstanceImpl(instance)
    }
  }

  protected def writeInstanceImpl(instance: Instance): Try[Instance]

  /**
    * Deletes an existing instance. Returns a failure if the instance could not be deleted.
    */
  def deleteInstance(toDelete: Instance): Try[Instance] = TimeLogger.info(s"deleteInstance(${toDelete.id})") {
    ifNotClosed {
      deleteInstanceImpl(toDelete)
    }
  }

  protected def deleteInstanceImpl(toDelete: Instance): Try[Instance]

  protected var closed: Boolean = false

  def isClosed: Boolean = closed

  def close(): Unit = TimeLogger.info(s"close()") {
    closeImpl()
    closed = true
  }

  protected def closeImpl(): Unit

}