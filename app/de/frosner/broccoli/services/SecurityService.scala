package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import com.typesafe.config.{ConfigObject, ConfigValueType}
import de.frosner.broccoli.controllers.Account
import de.frosner.broccoli.conf
import de.frosner.broccoli.conf.IllegalConfigException
import de.frosner.broccoli.util.Logging
import play.api.Configuration

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Singleton()
class SecurityService @Inject() (configuration: Configuration) extends Logging {

  // TODO check if setting it wrong will exit also in production
  private lazy val sessionTimeOutString: Option[String] = configuration.getString(conf.AUTH_SESSION_TIMEOUT_KEY)
  private lazy val sessionTimeOutTry: Try[Int] = sessionTimeOutString match {
    case Some(string) => Try(string.toInt).flatMap {
      int => if (int >= 1) Success(int) else Failure(new Exception())
    }
    case None => Success(conf.AUTH_SESSION_TIMEOUT_DEFAULT)
  }
  if (sessionTimeOutTry.isFailure) {
    Logger.error(s"Invalid ${conf.AUTH_SESSION_TIMEOUT_DEFAULT} specified: '${sessionTimeOutString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  lazy val sessionTimeoutInSeconds = sessionTimeOutTry.get
  Logger.info(s"${conf.AUTH_SESSION_TIMEOUT_KEY}=$sessionTimeoutInSeconds")

  private lazy val accounts: Set[Account] = {
    val tryAccounts = Try {
      configuration.getList(conf.AUTH_MODE_CONF_ACCOUNTS_KEY).map { users =>
        users.asScala.map { potentialUserObject =>
          potentialUserObject.valueType() match {
            case ConfigValueType.OBJECT => {
              val userObject = potentialUserObject.asInstanceOf[ConfigObject]
              Account(
                name = userObject.get(conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY).unwrapped().asInstanceOf[String],
                password = userObject.get(conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY).unwrapped().asInstanceOf[String]
              )
            }
            case valueType => {
              throw new IllegalConfigException(conf.AUTH_MODE_CONF_ACCOUNTS_KEY, s"Expected ${ConfigValueType.OBJECT} but got $valueType")
            }
          }
        }
      }.getOrElse(conf.AUTH_MODE_CONF_ACCOUNTS_DEFAULT)
    }
    val accounts = tryAccounts match {
      case Success(userObjects) => userObjects.toSet
      case Failure(throwable) => {
        Logger.error(s"Error parsing ${conf.AUTH_MODE_CONF_ACCOUNTS_KEY}: $throwable")
        System.exit(1)
        throw throwable
      }
    }
    Logger.info(s"Extracted ${accounts.size} accounts from ${conf.AUTH_MODE_CONF_ACCOUNTS_KEY}")
    accounts
  }

  def isAllowedToAuthenticate(account: Account): Boolean = accounts.contains(account)

  def getAccount(id: String): Option[Account] = accounts.find(_.name == id)

}
