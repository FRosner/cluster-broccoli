package de.frosner.broccoli.services

import java.io._

import de.frosner.broccoli.models.Instance
import de.frosner.broccoli.util.Logging
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

@volatile
case class FileSystemInstanceStorage(storageDirectory: File, prefix: String) extends InstanceStorage with Logging {

  import Instance.instancePersistenceWrites
  import Instance.instancePersistenceReads

  require(storageDirectory.isDirectory && storageDirectory.canWrite, s"'${storageDirectory}' needs to be a writable directory")
  private val lock = new File(storageDirectory, ".lock")
  Logger.info(s"Locking $storageDirectory ($lock)")
  if (!lock.createNewFile()) {
    throw new IllegalStateException(s"Cannot lock $storageDirectory. Is there another Broccoli instance running?")
  }

  def idToFile(id: String): File = new File(storageDirectory, id + ".json")

  override def closeImpl(): Unit = {
    if (lock.delete()) {
      Logger.info(s"Releasing lock on '$storageDirectory' ('$lock')")
      closed = true
    } else {
      Logger.error(s"Could not release lock on '$storageDirectory' ('$lock')")
    }
  }

  override def readInstanceImpl(id: String): Try[Instance] = {
    val input = Try(new FileInputStream(idToFile(id)))
    val instance = input.map(i => Json.parse(i).as[Instance])
    input.foreach(_.close())
    instance.flatMap { instance =>
      if (instance.id != id) {
        val error = s"Instance id (${instance.id}) does not match file name ($id)"
        Logger.error(error)
        Failure(new IllegalStateException(error))
      } else {
        Success(instance)
      }
    }
  }

  @volatile
  protected override def readInstancesImpl(): Try[Set[Instance]] = {
    readInstances(_ => true)
  }

  @volatile
  override def readInstancesImpl(idFilter: String => Boolean): Try[Set[Instance]] = {
    val instanceIds = Try {
      val instanceFiles = storageDirectory.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = {
          val fileName = pathname.getName
          val id = fileName.stripSuffix(".json")
          fileName.endsWith(".json") && idFilter(id) && id.startsWith(prefix)
        }
      })
      instanceFiles.map(_.getName.stripSuffix(".json"))
    }
    instanceIds.map(_.map { id =>
      val tryInstance = readInstanceImpl(id)
      tryInstance match {
        case Success(instance) => instance
        case Failure(throwable) => throw throwable
      }
    }.toSet)
  }

  @volatile
  override def writeInstanceImpl(instance: Instance): Try[Instance] = {
    val id = instance.id
    val file = idToFile(id)
    val printStream = Try(new PrintStream(new FileOutputStream(file)))
    val afterWrite = printStream.map(_.append(Json.toJson(instance).toString()))
    printStream.map(_.close())
    afterWrite.map(_ => instance)
  }

  @volatile
  override def deleteInstanceImpl(toDelete: Instance): Try[Instance] = {
    val id = toDelete.id
    val file = idToFile(id)
    val deleted = Try(file.delete())
    deleted.flatMap { success =>
      if (success) Success(toDelete) else Failure(new FileNotFoundException(s"Could not delete $file"))
    }
  }

}