package de.frosner.broccoli.templates

import java.io.File
import java.nio.file.Path

import de.frosner.broccoli.util.TemporaryDirectoryContext
import org.apache.commons.io.FileUtils
import org.specs2.execute.{AsResult, Result}

trait TemporaryTemplatesContext extends TemporaryDirectoryContext {
  override protected def foreach[R: AsResult](f: (Path) => R): Result = super.foreach { path: Path =>
    val templatesPath = getClass.getResource("/de/frosner/broccoli/templates")
    FileUtils.copyDirectoryToDirectory(new File(templatesPath.getFile), path.toFile)
    f(path.resolve("templates"))
  }
}
