package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.conf
import play.api.{Configuration, Logger}

@Singleton
class PermissionsService @Inject()(configuration: Configuration) {

  private lazy val permissionsMode: String = {
    val maybePermissionsMode = configuration.getString(conf.PERMISSIONS_MODE_KEY)
    val result = maybePermissionsMode match {
      case Some(mode) => {
        if (Set(conf.PERMISSIONS_MODE_ADMINISTRATOR, conf.PERMISSIONS_MODE_OPERATOR, conf.PERMISSIONS_MODE_USER).contains(mode)) {
          mode
        } else {
          val errorMessage = s"Invalid ${conf.PERMISSIONS_MODE_KEY}: $mode"
          Logger.error(errorMessage)
          System.exit(1)
          throw new IllegalArgumentException(errorMessage)
        }
      }
      case None => {
        Logger.info(s"No ${conf.PERMISSIONS_MODE_KEY} specified, using default.")
        conf.PERMISSIONS_MODE_DEFAULT
      }
    }
    Logger.info(s"${conf.PERMISSIONS_MODE_KEY}=$result")
    result
  }

  def getPermissionsMode(): String = permissionsMode

}
