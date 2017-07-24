package de.frosner.broccoli.util

import java.io.IOException
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}
import java.nio.file.attribute.BasicFileAttributes

import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.ForEach

/**
  * Provide a temporary directory to each test, automatically cleaning up after the test.
  */
trait TemporaryDirectoryContext extends ForEach[Path] {
  override protected def foreach[R: AsResult](f: (Path) => R): Result = {
    val tempDirectory = Files.createTempDirectory(getClass.getName)
    try {
      AsResult(f(tempDirectory))
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
}
