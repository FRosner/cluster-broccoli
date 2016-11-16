package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import com.typesafe.config.{ConfigObject, ConfigValueFactory, ConfigValueType}
import de.frosner.broccoli.conf
import de.frosner.broccoli.conf.IllegalConfigException
import de.frosner.broccoli.models.{Account, Credentials, Role, UserAccount}
import de.frosner.broccoli.util.Logging
import play.api.Configuration

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Singleton()
case class SecurityService @Inject() (configuration: Configuration) extends Logging {

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

  lazy val authMode: String = {
    val maybeAuthMode = configuration.getString(conf.AUTH_MODE_KEY)
    val result = maybeAuthMode match {
      case Some(mode) => {
        if (Set(conf.AUTH_MODE_CONF, conf.AUTH_MODE_NONE).contains(mode)) {
          mode
        } else {
          val errorMessage = s"Invalid ${conf.AUTH_MODE_KEY}: $mode"
          Logger.error(errorMessage)
          System.exit(1)
          throw new IllegalArgumentException(errorMessage)
        }
      }
      case None => {
        conf.AUTH_MODE_DEFAULT
      }
    }
    Logger.info(s"${conf.AUTH_MODE_KEY}=$result")
    result
  }

  private lazy val accounts: Set[Account] = {
    val accounts = SecurityService.tryAccounts(configuration) match {
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

  def isAllowedToAuthenticate(credentials: Credentials): Boolean = accounts.exists {
    account => account.name == credentials.name && account.password == credentials.password
  }

  def getAccount(id: String): Option[Account] = accounts.find(_.name == id)

}

object SecurityService {

  private[services] def tryAccounts(configuration: Configuration): Try[Iterable[Account]] = Try {
    configuration.getList(conf.AUTH_MODE_CONF_ACCOUNTS_KEY).map { users =>
      users.asScala.map { potentialUserObject =>
        potentialUserObject.valueType() match {
          case ConfigValueType.OBJECT => {
            val userObject = potentialUserObject.asInstanceOf[ConfigObject]
            UserAccount(
              name = userObject.get(conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY).unwrapped().asInstanceOf[String],
              password = userObject.get(conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY).unwrapped().asInstanceOf[String],
              instanceRegex = userObject.getOrDefault(
                conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY,
                ConfigValueFactory.fromAnyRef(conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_DEFAULT)
              ).unwrapped().asInstanceOf[String],
              role = Role.withName(
                userObject.getOrDefault(
                  conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY,
                  ConfigValueFactory.fromAnyRef(conf.AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT.toString)
                ).unwrapped().asInstanceOf[String]
              )
            )
          }
          case valueType => {
            throw new IllegalConfigException(conf.AUTH_MODE_CONF_ACCOUNTS_KEY, s"Expected ${ConfigValueType.OBJECT} but got $valueType")
          }
        }
      }
    }.getOrElse(conf.AUTH_MODE_CONF_ACCOUNTS_DEFAULT)
  }

}
