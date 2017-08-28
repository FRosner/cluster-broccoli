package de.frosner.broccoli.instances.storage.filesystem

import com.google.inject.{AbstractModule, Provides, Singleton}
import de.frosner.broccoli.BroccoliConfiguration
import de.frosner.broccoli.instances.storage.InstanceStorage
import net.codingwell.scalaguice.ScalaModule
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
  * Module to store instances on the file system.
  */
class FileSystemStorageModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provide the file system instance storage.
    *
    * @param config Broccoli's configuration
    * @param applicationLifecycle The application lifecycle to shutdown the storage
    * @return A filesystem storage for instances
    */
  @Provides
  @Singleton
  def provideFileSystemInstanceStorage(
      config: BroccoliConfiguration,
      applicationLifecycle: ApplicationLifecycle
  ): InstanceStorage = {
    val storage = new FileSystemInstanceStorage(config.instances.storage.fs.path.toAbsolutePath.toFile)
    applicationLifecycle.addStopHook(() => {
      if (!storage.isClosed) {
        storage.close()
      }
      Future.successful({})
    })
    storage
  }
}
