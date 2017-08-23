package de.frosner.broccoli.instances.storage

import de.frosner.broccoli.logging
import de.frosner.broccoli.models.Instance
import play.api.Logger

import scala.util.Try

trait InstanceStorage {

  protected def log: Logger

  private def ifNotClosed[T](f: => T): T =
    if (!isClosed) {
      f
    } else {
      throw new IllegalStateException(s"$this has already been closed.")
    }

  /**
    * Reads the instance with the specified ID.
    */
  def readInstance(id: String): Try[Instance] =
    logging.logExecutionTime(s"readInstance($id)") {
      ifNotClosed {
        readInstanceImpl(id)
      }
    }(log.info(_))

  protected def readInstanceImpl(id: String): Try[Instance]

  /**
    * Reads all instances.
    */
  def readInstances(): Try[Set[Instance]] =
    logging.logExecutionTime("readInstances()") {
      ifNotClosed {
        readInstancesImpl
      }
    }(log.info(_))

  protected def readInstancesImpl: Try[Set[Instance]]

  /**
    * Reads all instances whose IDs match the given filter.
    */
  def readInstances(idFilter: String => Boolean): Try[Set[Instance]] =
    logging.logExecutionTime("readInstances(idFilter)") {
      ifNotClosed {
        readInstancesImpl(idFilter)
      }
    }(log.info(_))

  protected def readInstancesImpl(idFilter: String => Boolean): Try[Set[Instance]]

  /**
    * Persists an instance.
    */
  def writeInstance(instance: Instance): Try[Instance] =
    logging.logExecutionTime(s"writeInstance(${instance.id})") {
      ifNotClosed {
        writeInstanceImpl(instance)
      }
    }(log.info(_))

  protected def writeInstanceImpl(instance: Instance): Try[Instance]

  /**
    * Deletes an existing instance. Returns a failure if the instance could not be deleted.
    */
  def deleteInstance(toDelete: Instance): Try[Instance] =
    logging.logExecutionTime(s"deleteInstance(${toDelete.id})") {
      ifNotClosed {
        deleteInstanceImpl(toDelete)
      }
    }(log.info(_))

  protected def deleteInstanceImpl(toDelete: Instance): Try[Instance]

  protected var closed: Boolean = false

  def isClosed: Boolean = closed

  def close(): Unit =
    logging.logExecutionTime("close()") {
      closeImpl()
      closed = true
    }(log.info(_))

  protected def closeImpl(): Unit

}
