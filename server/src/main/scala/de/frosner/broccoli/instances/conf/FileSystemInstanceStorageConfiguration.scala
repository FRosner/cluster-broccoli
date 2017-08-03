package de.frosner.broccoli.instances.conf

import com.typesafe.config.Config

/**
  * Configuration for FileSystemInstanceStorage
  *
  * @param url location on the filesystem to store the instance information
  */
final case class FileSystemInstanceStorageConfiguration(url: String)

object FileSystemInstanceStorageConfiguration {
  protected val log = play.api.Logger(getClass)

  def fromConfig(config: Config): FileSystemInstanceStorageConfiguration = {
    val url = config.getString("url")
    log.info(s"broccoli.instances.storage.fs.url=$url")
    FileSystemInstanceStorageConfiguration(url)
  }
}
