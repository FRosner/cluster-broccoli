package de.frosner.broccoli.services

import java.io.{File, FileNotFoundException, FileOutputStream, PrintStream}
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import de.frosner.broccoli.models.{Instance, Template}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.BeforeAfterAll

import scala.io.Source
import scala.util.{Failure, Success, Try}

class FileSystemInstanceStorageSpec extends Specification {

  val testRoot = new File("test/resources/de/frosner/broccoli/services")
  require(testRoot.isDirectory && testRoot.canRead && testRoot.canWrite,
    s"Cannot use '$testRoot' properly. Current working dir is '${Paths.get("").toAbsolutePath()}'.")

  def usingTempFolder[T](f: File => T): T = {
    val tempFolder = new File(testRoot, UUID.randomUUID().toString)
    val mkdir = Try(tempFolder.mkdir())
    def deleteRecursively(file: File): Unit = {
      if (file.isDirectory)
        file.listFiles.foreach(deleteRecursively)
      if (file.exists && !file.delete)
        throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
    }
    val result = mkdir.map(_ => f(tempFolder))
    deleteRecursively(tempFolder)
    result match {
      case Success(r) => r
      case Failure(t) => throw t
    }
  }

  val instance = Instance(
    id = "prefix-id",
    template = Template(id = "t", template = "{{id}}" ,description = "d", parameterInfos = Map.empty),
    parameterValues = Map(
      "id" -> "prefix-id"
    )
  )

  "Using the file system instance storage" should {

    "require the storage directory to be present" in {
      FileSystemInstanceStorage(new File(UUID.randomUUID().toString), "") should throwA[IllegalArgumentException]
    }

    "fail if the storage directory is already locked" in {
      usingTempFolder { folder =>
        FileSystemInstanceStorage(folder, "")
        FileSystemInstanceStorage(folder, "") should throwA[IllegalStateException]
      }
    }

    "lock the storage directory" in {
      usingTempFolder { folder =>
        FileSystemInstanceStorage(folder, "")
        new File(folder, ".lock").isFile === true
      }
    }

    "unlock the storage directory on close" in {
      usingTempFolder { folder =>
        FileSystemInstanceStorage(folder, "").close()
        new File(folder, ".lock").isFile === false
      }
    }

  }

  "Writing and reading an instance" should {

    "work" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        storage.readInstance(instance.id) === Success(instance)
      }
    }

    "fail if the file is not readable" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        val instanceFile = new File(folder, instance.id + ".json")
        instanceFile.setReadable(false)
        storage.readInstance(instance.id).failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

    "fail if the file is not a valid JSON" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        val instanceFile = new File(folder, instance.id + ".json")
        val writer = new PrintStream(new FileOutputStream(instanceFile))
        writer.println("}")
        writer.close()
        storage.readInstance(instance.id).failed.get should beAnInstanceOf[JsonParseException]
      }
    }

    "fail if the instance ID does not match the file name" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        val instanceFile = new File(folder, instance.id + ".json")
        val fileContent = Source.fromFile(instanceFile).getLines().mkString("").replaceFirst("^\\{\"id\"\\:\"prefix\\-id\"", "{\"id\":\"not-matching\"")
        val writer = new PrintStream(new FileOutputStream(instanceFile))
        writer.println(fileContent)
        writer.close()
        storage.readInstance(instance.id).failed.get should beAnInstanceOf[IllegalStateException]
      }
    }

  }

  "Writing and reading all instances" should {

    "not apply an additional filter if called without one" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        val instance2 = instance.copy(id = instance.id + "2")
        storage.writeInstance(instance)
        storage.writeInstance(instance2)

        storage.readInstances === Success(Set(instance, instance2))
      }
    }

    "apply an additional filter if specified" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        val instance2 = instance.copy(id = instance.id + "2")
        storage.writeInstance(instance)
        storage.writeInstance(instance2)

        storage.readInstances(_.endsWith("2")) === Success(Set(instance2))
      }
    }

    "filter out non .json files from the directory" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        val newFile = new File(folder, "notJson.exe")
        newFile.createNewFile()
        storage.readInstances === Success(Set.empty[Instance])
      }
    }

    "filter out instances not matching the prefix" in {
      usingTempFolder { folder =>
        val storage1 = FileSystemInstanceStorage(folder, "")
        storage1.writeInstance(instance)
        storage1.close()

        val storage2 = FileSystemInstanceStorage(folder, "notmatchingprefix")

        storage2.readInstances === Success(Set.empty)
      }
    }

    "fail if reading any of the instances fails" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        val instanceFile = new File(folder, instance.id + ".json")
        instanceFile.setReadable(false)
        storage.readInstances.failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

  }

  "Deleting an instance" should {

    "work" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.writeInstance(instance)
        storage.deleteInstance(instance)
        storage.readInstance(instance.id).isFailure === true
      }
    }

    "fail if the file does not exist" in {
      usingTempFolder { folder =>
        val storage = FileSystemInstanceStorage(folder, "")
        storage.deleteInstance(instance).failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

  }

}
