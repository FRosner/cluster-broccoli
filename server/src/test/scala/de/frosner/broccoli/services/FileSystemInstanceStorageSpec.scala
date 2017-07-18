package de.frosner.broccoli.services

import java.io._
import java.lang.IllegalArgumentException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import de.frosner.broccoli.models.{Instance, Template}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.BeforeAfterAll

import scala.io.Source
import scala.util.{Failure, Success, Try}

class FileSystemInstanceStorageSpec extends Specification {

  val testRoot = Files.createTempDirectory(getClass.getName)

  def withTempDirectory[T](f: Path => T): T = {
    val tempDirectory = Files.createTempDirectory(getClass.getName)
    try {
      f(tempDirectory)
    } finally {
      Files.walkFileTree(
        tempDirectory,
        new FileVisitor[Path] {
          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          }

          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = throw exc

          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
            FileVisitResult.CONTINUE
        }
      )
    }
  }

  val instance = Instance(
    id = "prefix-id",
    template = Template(id = "t", template = "{{id}}", description = "d", parameterInfos = Map.empty),
    parameterValues = Map(
      "id" -> "prefix-id"
    )
  )

  "Using the file system instance storage" should {

    "require the storage directory to be present" in {
      FileSystemInstanceStorage(new File(UUID.randomUUID().toString)) should throwA[IllegalArgumentException]
    }

    "fail if the storage directory is already locked" in {
      withTempDirectory { folder =>
        FileSystemInstanceStorage(folder.toFile)
        FileSystemInstanceStorage(folder.toFile) should throwA[IllegalStateException]
      }
    }

    "lock the storage directory" in {
      withTempDirectory { folder =>
        FileSystemInstanceStorage(folder.toFile)
        new File(folder.toFile, ".lock").isFile === true
      }
    }

    "unlock the storage directory on close" in {
      withTempDirectory { folder =>
        FileSystemInstanceStorage(folder.toFile).close()
        new File(folder.toFile, ".lock").isFile === false
      }
    }

  }

  "Writing and reading an instance" should {

    "work" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.writeInstance(instance)
        storage.readInstance(instance.id) === Success(instance)
      }
    }

    "fail if the file is not readable" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.writeInstance(instance)
        val instanceFile = new File(folder.toFile, instance.id + ".json")
        instanceFile.setReadable(false)
        storage.readInstance(instance.id).failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

    "fail if the file is not a valid JSON" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.writeInstance(instance)
        val instanceFile = new File(folder.toFile, instance.id + ".json")
        val writer = new PrintStream(new FileOutputStream(instanceFile))
        writer.println("}")
        writer.close()
        storage.readInstance(instance.id).failed.get should beAnInstanceOf[JsonParseException]
      }
    }

    "fail if the instance ID does not match the file name" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
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

  }

  "Writing and reading all instances" should {

    "not apply an additional filter if called without one" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        val instance2 = instance.copy(id = instance.id + "2")
        storage.writeInstance(instance)
        storage.writeInstance(instance2)

        storage.readInstances === Success(Set(instance, instance2))
      }
    }

    "apply an additional filter if specified" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        val instance2 = instance.copy(id = instance.id + "2")
        storage.writeInstance(instance)
        storage.writeInstance(instance2)

        storage.readInstances(_.endsWith("2")) === Success(Set(instance2))
      }
    }

    "filter out non .json files from the directory" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        val newFile = new File(folder.toFile, "notJson.exe")
        newFile.createNewFile()
        storage.readInstances === Success(Set.empty[Instance])
      }
    }

    "fail if reading any of the instances fails" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.writeInstance(instance)
        val instanceFile = new File(folder.toFile, instance.id + ".json")
        instanceFile.setReadable(false)
        storage.readInstances.failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

  }

  "Deleting an instance" should {

    "work" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.writeInstance(instance)
        storage.deleteInstance(instance)
        storage.readInstance(instance.id).isFailure === true
      }
    }

    "fail if the file does not exist" in {
      withTempDirectory { folder =>
        val storage = FileSystemInstanceStorage(folder.toFile)
        storage.deleteInstance(instance).failed.get should beAnInstanceOf[FileNotFoundException]
      }
    }

  }

}
