package de.frosner.broccoli.instances

import java.nio.file.FileSystems
import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import de.frosner.broccoli.instances.conf.InstanceConfiguration
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class InstanceModule extends AbstractModule with ScalaModule {
  protected val log = play.api.Logger(getClass)
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideInstanceConfiguration(config: Configuration): InstanceConfiguration =
    InstanceConfiguration.fromConfig(config.underlying.getConfig("broccoli.instances"))

  @Provides
  @Singleton
  def provideInstanceStorage(instanceConfig: InstanceConfiguration,
                             ws: WSClient,
                             applicationLifecycle: ApplicationLifecycle): InstanceStorage = {
    val maybeInstanceStorage = instanceConfig.storageConfiguration.storageType match {
      case StorageType.FileSystem => {
        val config = instanceConfig.storageConfiguration.fsConfig
        val path = FileSystems.getDefault.getPath(config.url).toAbsolutePath
        Try(FileSystemInstanceStorage(path.toFile))
      }
      case StorageType.CouchDB => {
        val config = instanceConfig.storageConfiguration.couchDBConfig
        Try(CouchDBInstanceStorage(config.url, config.dbName, ws))
      }
      case invalidStorageType => throw new IllegalArgumentException("Wrong instance storage type")
    }

    val instanceStorage = maybeInstanceStorage match {
      case Success(storage) => storage
      case Failure(throwable) =>
        log.error(s"Cannot start instance storage: $throwable")
        throw throwable
    }

    sys.addShutdownHook {
      log.info("Closing instanceStorage (shutdown hook)")
      if (!instanceStorage.isClosed) {
        instanceStorage.close()
      }
    }
    implicit val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
    applicationLifecycle.addStopHook(() =>
      Future {
        log.info("Closing instanceStorage (stop hook)")
        if (!instanceStorage.isClosed) {
          instanceStorage.close()
        }
    })
    instanceStorage
  }

}
