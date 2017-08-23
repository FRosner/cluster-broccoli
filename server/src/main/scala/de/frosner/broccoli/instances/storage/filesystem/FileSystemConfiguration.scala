package de.frosner.broccoli.instances.storage.filesystem

import java.nio.file.Path

/**
  * Configuration for FileSystemInstanceStorage
  *
  * @param path location on the filesystem to store the instance information
  */
final case class FileSystemConfiguration(path: Path)
