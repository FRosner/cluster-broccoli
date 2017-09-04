package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.util.Credentials
import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.conf
import de.frosner.broccoli.conf.IllegalConfigException
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Singleton()
case class SecurityService @Inject()(configuration: Configuration) {

  private val log = play.api.Logger(getClass)

  // TODO check if setting it wrong will exit also in production
  private lazy val sessionTimeOutString: Option[String] = configuration.getString(conf.AUTH_SESSION_TIMEOUT_KEY)
  private lazy val sessionTimeOutTry: Try[Int] = sessionTimeOutString match {
    case Some(string) =>
      Try(string.toInt).flatMap { int =>
        if (int >= 1) Success(int) else Failure(new Exception())
      }
    case None => Success(conf.AUTH_SESSION_TIMEOUT_DEFAULT)
  }
  if (sessionTimeOutTry.isFailure) {
    log.error(
      s"Invalid ${conf.AUTH_SESSION_TIMEOUT_DEFAULT} specified: '${sessionTimeOutString.get}'. Needs to be a positive integer.")
    System.exit(1)
  }
  lazy val sessionTimeoutInSeconds = sessionTimeOutTry.get
  log.info(s"${conf.AUTH_SESSION_TIMEOUT_KEY}=$sessionTimeoutInSeconds")

  lazy val allowedFailedLogins = SecurityService.tryAllowedFailedLogins(configuration) match {
    case Success(value) => {
      log.info(s"${conf.AUTH_ALLOWED_FAILED_LOGINS_KEY}=$value")
      value
    }
    case Failure(throwable) => {
      log.error(throwable.toString)
      System.exit(1)
      throw throwable
    }
  }

  lazy val authMode: String = {
    val maybeAuthMode = configuration.getString(conf.AUTH_MODE_KEY)
    val result = maybeAuthMode match {
      case Some(mode) => {
        if (Set(conf.AUTH_MODE_CONF, conf.AUTH_MODE_NONE).contains(mode)) {
          mode
        } else {
          val errorMessage = s"Invalid ${conf.AUTH_MODE_KEY}: $mode"
          log.error(errorMessage)
          System.exit(1)
          throw new IllegalArgumentException(errorMessage)
        }
      }
      case None => {
        conf.AUTH_MODE_DEFAULT
      }
    }
    log.info(s"${conf.AUTH_MODE_KEY}=$result")
    result
  }

  lazy val cookieSecure: Boolean = {
    val parsed = SecurityService.tryCookieSecure(configuration) match {
      case Success(value) => value
      case Failure(throwable) =>
        log.error(s"Error parsing ${conf.AUTH_COOKIE_SECURE_KEY}: $throwable")
        System.exit(1)
        throw throwable
    }
    log.info(s"${conf.AUTH_COOKIE_SECURE_KEY}=$parsed")
    parsed
  }

  lazy val allowMultiLogin: Boolean = {
    val parsed = SecurityService.tryAllowMultiLogin(configuration) match {
      case Success(value) => value
      case Failure(throwable) =>
        log.error(s"Error parsing ${conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY}: $throwable")
        System.exit(1)
        throw throwable
    }
    log.info(s"${conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY}=$parsed")
    parsed
  }

  private lazy val accounts: Set[Account] = {
    val accounts = SecurityService.tryAccounts(configuration) match {
      case Success(userObjects) => userObjects.toSet
      case Failure(throwable) => {
        log.error(s"Error parsing ${conf.AUTH_MODE_CONF_ACCOUNTS_KEY}: $throwable")
        System.exit(1)
        throw throwable
      }
    }
    log.info(s"Extracted ${accounts.size} accounts from ${conf.AUTH_MODE_CONF_ACCOUNTS_KEY}")
    accounts
  }

  @volatile
  private var failedLoginAttempts: Map[String, Int] = Map.empty

  // TODO store failed logins (reset on successful login) and only allowToAuthenticate if not blocked

  def isAllowedToAuthenticate(credentials: Credentials): Boolean = {
    val credentialsFailedLoginAttempts = failedLoginAttempts.getOrElse(credentials.identifier, 0)
    val allowed = if (credentialsFailedLoginAttempts <= allowedFailedLogins) {
      accounts.exists { account =>
        account.name == credentials.identifier && account.password == credentials.password
      }
    } else {
      log.warn(
        s"Credentials for '${credentials.identifier}' exceeded the allowed number of failed logins: " +
          s"$allowedFailedLogins (has $credentialsFailedLoginAttempts)")
      false
    }
    if (!allowed) {
      failedLoginAttempts = failedLoginAttempts.updated(credentials.identifier, credentialsFailedLoginAttempts + 1)
    }
    allowed
  }

  def getAccount(id: String): Option[Account] = accounts.find(_.name == id)

}

object SecurityService {

  private[services] def tryAllowMultiLogin(configuration: Configuration): Try[Boolean] = Try {
    configuration
      .getBoolean(conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY)
      .getOrElse(conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_DEFAULT)
  }

  private[services] def tryAccounts(configuration: Configuration): Try[Iterable[Account]] = Try {
    if (configuration.underlying.hasPath(conf.AUTH_MODE_CONF_ACCOUNTS_KEY)) {
      configuration.underlying
        .getConfigList(conf.AUTH_MODE_CONF_ACCOUNTS_KEY)
        .asScala
        .map { account =>
          Account(
            name = account.getString(conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY),
            password = account.getString(conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY),
            instanceRegex = account.getString(conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY),
            role = if (account.hasPath(conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY)) {
              Role.withNameInsensitive(account.getString(conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY))
            } else {
              conf.AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT
            }
          )
        }
    } else {
      conf.AUTH_MODE_CONF_ACCOUNTS_DEFAULT
    }
  }

  private[services] def tryCookieSecure(configuration: Configuration): Try[Boolean] = Try {
    configuration.getBoolean(conf.AUTH_COOKIE_SECURE_KEY).getOrElse(conf.AUTH_COOKIE_SECURE_DEFAULT)
  }

  private[services] def tryAllowedFailedLogins(configuration: Configuration): Try[Int] = Try {
    val stringOption = configuration.getInt(conf.AUTH_ALLOWED_FAILED_LOGINS_KEY)
    stringOption match {
      case Some(int) => {
        if (int >= 1)
          int
        else
          throw new IllegalConfigException(conf.AUTH_ALLOWED_FAILED_LOGINS_KEY, "Must be a positive Integer.")
      }
      case None => conf.AUTH_ALLOWED_FAILED_LOGINS_DEFAULT
    }
  }

}
