package de.frosner.broccoli.instances

import java.nio.file.FileSystems
import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import de.frosner.broccoli.BroccoliConfiguration
import de.frosner.broccoli.instances.conf.InstanceConfiguration
import de.frosner.broccoli.templates.TemplateRenderer
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Provide instance storage and template rendering implementations.
  */
class InstanceModule extends AbstractModule with ScalaModule {
  private val log = Logger(getClass)

  override def configure(): Unit = {}

  /**
    * Provides the template renderer for instances.
    */
  @Provides
  @Singleton
  def provideTemplateRenderer(config: BroccoliConfiguration): TemplateRenderer =
    new TemplateRenderer(config.instances.parameters.defaultType)

  /**
    * Provides the instance storage.
    */
  @Provides
  @Singleton
  def provideInstanceStorage(config: BroccoliConfiguration,
                             ws: WSClient,
                             applicationLifecycle: ApplicationLifecycle): InstanceStorage = {
    val storageConfig = config.instances.storage
    val instanceStorage = storageConfig.`type` match {
      case StorageType.FileSystem => {
        val config = storageConfig.fs
        val path = FileSystems.getDefault.getPath(config.url).toAbsolutePath
        FileSystemInstanceStorage(path.toFile)
      }
      case StorageType.CouchDB => {
        val config = storageConfig.couchdb
        CouchDBInstanceStorage(config.url, config.database, ws)
      }
    }

    applicationLifecycle.addStopHook(() => {
      log.info("Closing instanceStorage (stop hook)")
      if (!instanceStorage.isClosed) {
        instanceStorage.close()
      }
      Future.successful({})
    })
    instanceStorage
  }

}
