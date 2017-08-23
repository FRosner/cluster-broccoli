package de.frosner.broccoli.instances.storage.couchdb

import com.google.inject.{AbstractModule, Provides, Singleton}
import de.frosner.broccoli.BroccoliConfiguration
import de.frosner.broccoli.instances.storage.InstanceStorage
import net.codingwell.scalaguice.ScalaModule
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Module to store instances in CouchDB.
  */
class CouchDBStorageModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  /**
    * Provides an instance storage using CouchDB.
    *
    * @param config Broccoli's configuration
    * @param client Web client to access CouchDB
    * @param applicationLifecycle The application lifecyle to shutdown the storage
    * @return An instance storage using CouchDB
    */
  @Provides
  @Singleton
  def provideCouchDBInstanceStorage(
      config: BroccoliConfiguration,
      client: WSClient,
      applicationLifecycle: ApplicationLifecycle
  ): InstanceStorage = {
    val couchdbConfig = config.instances.storage.couchdb
    val storage = new CouchDBInstanceStorage(couchdbConfig.url, couchdbConfig.database, client)
    applicationLifecycle.addStopHook(() => {
      if (!storage.isClosed) {
        storage.close()
      }
      Future.successful({})
    })
    storage
  }
}
