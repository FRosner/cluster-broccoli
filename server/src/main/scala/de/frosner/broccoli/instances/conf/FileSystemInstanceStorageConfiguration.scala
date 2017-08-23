package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config
import play.api.Logger

/**
  * Configuration for FileSystemInstanceStorage
  *
  * @param url location on the filesystem to store the instance information
  */
final case class FileSystemInstanceStorageConfiguration(url: String)
