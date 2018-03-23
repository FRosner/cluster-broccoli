package de.frosner.broccoli.instances.storage.filesystem

import java.io._
import java.nio.file._
import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import de.frosner.broccoli.models.{Instance, StringParameterValue, Template}
import de.frosner.broccoli.util.TemporaryDirectoryContext
import org.specs2.mutable.Specification

import scala.io.Source
import scala.util.Success

class FileSystemInstanceStorageSpec extends Specification with TemporaryDirectoryContext {

  val instance = Instance(
    id = "prefix-id",
    template = Template(id = "t", template = "{{id}}", description = "d", parameterInfos = Map.empty),
    parameterValues = Map(
      "id" -> StringParameterValue("prefix-id")
    )
  )

  "Using the file system instance storage" should {

    "require the storage directory to be present" in {
      new FileSystemInstanceStorage(new File(UUID.randomUUID().toString)) should throwA[IllegalArgumentException]
    }

    "fail if the storage directory is already locked" in { folder: Path =>
      new FileSystemInstanceStorage(folder.toFile)
      new FileSystemInstanceStorage(folder.toFile) should throwA[IllegalStateException]
    }

    "lock the storage directory" in { folder: Path =>
      new FileSystemInstanceStorage(folder.toFile)
      new File(folder.toFile, ".lock").isFile === true
    }

    "unlock the storage directory on close" in { folder: Path =>
      new FileSystemInstanceStorage(folder.toFile).close()
      new File(folder.toFile, ".lock").isFile === false
    }

  }

  "Writing and reading an instance" should {

    "work" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      storage.readInstance(instance.id) === Success(instance)
    }

    "fail if the tmp file cannot be created" in { folder: Path =>
      val folderFile = folder.toFile
      val storage = new FileSystemInstanceStorage(folderFile)
      val instanceFile = new File(folderFile, instance.id + ".json_tmp")
      instanceFile.mkdir()
      storage.writeInstance(instance).failed.get should beAnInstanceOf[FileNotFoundException]
    }

    "fail if the tmp file cannot be moved" in { folder: Path =>
      val folderFile = folder.toFile
      val storage = new FileSystemInstanceStorage(folderFile)
      val instanceFile = new File(folderFile, instance.id + ".json")
      instanceFile.mkdir()
      storage.writeInstance(instance).failed.get should beAnInstanceOf[FileSystemException]
    }

    "fail if the file is not readable" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      val instanceFile = new File(folder.toFile, instance.id + ".json")
      instanceFile.setReadable(false)
      storage.readInstance(instance.id).failed.get should beAnInstanceOf[FileNotFoundException]
    }

    "fail if the file is not a valid JSON" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      val instanceFile = new File(folder.toFile, instance.id + ".json")
      val writer = new PrintStream(new FileOutputStream(instanceFile))
      writer.println("}")
      writer.close()
      storage.readInstance(instance.id).failed.get should beAnInstanceOf[JsonParseException]
    }

    "fail if the instance ID does not match the file name" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      val instanceFile = new File(folder.toFile, instance.id + ".json")
      val fileContent = Source
        .fromFile(instanceFile)
        .getLines()
        .mkString("")
        .replaceFirst("^\\{\"id\"\\:\"prefix\\-id\"", "{\"id\":\"not-matching\"")
      val writer = new PrintStream(new FileOutputStream(instanceFile))
      writer.println(fileContent)
      writer.close()
      storage.readInstance(instance.id).failed.get should beAnInstanceOf[IllegalStateException]
    }
  }

  "Writing and reading all instances" should {

    "not apply an additional filter if called without one" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      val instance2 = instance.copy(id = instance.id + "2")
      storage.writeInstance(instance)
      storage.writeInstance(instance2)

      storage.readInstances === Success(Set(instance, instance2))
    }

    "apply an additional filter if specified" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      val instance2 = instance.copy(id = instance.id + "2")
      storage.writeInstance(instance)
      storage.writeInstance(instance2)

      storage.readInstances(_.endsWith("2")) === Success(Set(instance2))
    }

    "filter out non .json files from the directory" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      val newFile = new File(folder.toFile, "notJson.exe")
      newFile.createNewFile()
      storage.readInstances === Success(Set.empty[Instance])
    }

    "fail if reading any of the instances fails" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      val instanceFile = new File(folder.toFile, instance.id + ".json")
      instanceFile.setReadable(false)
      storage.readInstances.failed.get should beAnInstanceOf[FileNotFoundException]
    }
  }

  "Deleting an instance" should {

    "work" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.writeInstance(instance)
      storage.deleteInstance(instance)
      storage.readInstance(instance.id).isFailure === true
    }

    "fail if the file does not exist" in { folder: Path =>
      val storage = new FileSystemInstanceStorage(folder.toFile)
      storage.deleteInstance(instance).failed.get should beAnInstanceOf[FileNotFoundException]
    }
  }

}
