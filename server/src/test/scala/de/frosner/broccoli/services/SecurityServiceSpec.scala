package de.frosner.broccoli.services

import com.google.common.collect.ImmutableMap
import com.mohiva.play.silhouette.api.util.Credentials
import com.typesafe.config.{ConfigException, ConfigFactory, ConfigValueFactory}
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Account, Role, UserAccount}
import org.specs2.mutable.Specification
import play.api.Configuration

import collection.JavaConverters._
import scala.util.Success

class SecurityServiceSpec extends Specification {

  def configWithAccounts(accounts: Iterable[UserAccount]): Configuration = {
    val accountsJava = accounts.map { account =>
      ImmutableMap.of(
        conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY,
        account.name,
        conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY,
        account.password,
        conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY,
        account.role.entryName,
        conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY,
        account.instanceRegex
      )
    }.asJava
    val config = ConfigFactory
      .empty()
      .withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
    Configuration(config)
  }

  val account = UserAccount("frank", "pass", "^test.*", Role.Administrator)

  "An authentication check" should {

    "succeed if the account matches" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(Credentials(account.name, account.password)) === true
    }

    "fail if the username does not exist" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(Credentials("new", account.password)) === false
    }

    "fail if the password does not matche" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(Credentials(account.name, "new")) === false
    }

    "succeed if the number of failed logins is equal to the allowed ones" in {
      val failedCredentials = Credentials(account.name, password = "new")
      val service = SecurityService(configWithAccounts(List(account)))
      val failedAttempts = for (attemptNo <- 1 to service.allowedFailedLogins) {
        service.isAllowedToAuthenticate(failedCredentials)
      }
      service.isAllowedToAuthenticate(Credentials(account.name, account.password)) === true
    }

    "fail if the number of failed logins is greater than the allowed number" in {
      val failedCredentials = Credentials(account.name, password = "new")
      val service = SecurityService(configWithAccounts(List(account)))
      val failedAttempts = for (attemptNo <- 0 to service.allowedFailedLogins) {
        service.isAllowedToAuthenticate(failedCredentials)
      }
      service.isAllowedToAuthenticate(Credentials(account.name, account.password)) === false
    }

  }

  "Finding accounts by id" should {

    "find an existing account" in {
      SecurityService(configWithAccounts(List(account)))
        .getAccount(account.name) === Some(account)
    }

    "not return anything if the name does not exist" in {
      SecurityService(configWithAccounts(List(account)))
        .getAccount("notExisting") === None
    }

  }

  "Parsing accounts from the configuration" should {

    "parse correctly" in {
      val accountsJava = Iterable {
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY,
          account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY,
          account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY,
          account.instanceRegex,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY,
          account.role.entryName
        )
      }.asJava
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
          ConfigValueFactory.fromIterable(accountsJava)
        )
      SecurityService.tryAccounts(Configuration(config)) should beSuccessfulTry.withValue(
        contain(exactly(account: Account)))
    }

    "not require the optional parameters" in {
      val accountsJava = Iterable {
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY,
          account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY,
          account.password
        )
      }.asJava
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
          ConfigValueFactory.fromIterable(accountsJava)
        )

      val userAccount: Account = UserAccount(
        name = account.name,
        password = account.password,
        instanceRegex = ".*",
        role = conf.AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT
      )
      SecurityService.tryAccounts(Configuration(config)) should beFailedTry.withThrowable[ConfigException.Missing](
        ".*instanceRegex.*")
    }

    "fail if the accounts are not a config list" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
          ConfigValueFactory.fromAnyRef("blub")
        )
      SecurityService.tryAccounts(Configuration(config)) should beFailedTry
    }

    "fail if each accounts element is not a config object" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
          ConfigValueFactory.fromAnyRef("blub")
        )
      SecurityService.tryAccounts(Configuration(config)) should beFailedTry
    }

    "if the role does not exist" in {
      val accountsJava = Iterable {
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY,
          account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY,
          account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY,
          "foo",
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY,
          account.instanceRegex
        )
      }.asJava
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
          ConfigValueFactory.fromIterable(accountsJava)
        )
      SecurityService.tryAccounts(Configuration(config)) should beFailedTry.withThrowable[NoSuchElementException](
        "foo is not a member of Enum .*")
    }

  }

  "Parsing cookie secure from the configuration" should {

    "work if the field is a boolean" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_COOKIE_SECURE_KEY,
          ConfigValueFactory.fromAnyRef(false)
        )
      SecurityService.tryCookieSecure(Configuration(config)) === Success(false)
    }

    "fail if the field is not a boolean" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_COOKIE_SECURE_KEY,
          ConfigValueFactory.fromAnyRef("bla")
        )
      SecurityService.tryCookieSecure(Configuration(config)) should beFailedTry
    }

    "take the default if the field is not defined" in {
      val config = ConfigFactory.empty()
      SecurityService.tryCookieSecure(Configuration(config)) === Success(conf.AUTH_COOKIE_SECURE_DEFAULT)
    }

  }

  "Parsing multi login from the configuration" should {

    "work if the field is a boolean" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY,
          ConfigValueFactory.fromAnyRef(false)
        )
      SecurityService.tryAllowMultiLogin(Configuration(config)) === Success(false)
    }

    "fail if the field is not a boolean" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY,
          ConfigValueFactory.fromAnyRef("bla")
        )
      SecurityService.tryAllowMultiLogin(Configuration(config)) should beFailedTry
    }

    "take the default if the field is not defined" in {
      val config = ConfigFactory.empty()
      SecurityService.tryAllowMultiLogin(Configuration(config)) === Success(conf.AUTH_SESSION_ALLOW_MULTI_LOGIN_DEFAULT)
    }

  }

  "Parsing allowed failed logins from the config" should {

    "work when the value is a positive integer" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_ALLOWED_FAILED_LOGINS_KEY,
          ConfigValueFactory.fromAnyRef(1)
        )
      SecurityService.tryAllowedFailedLogins(Configuration(config)) === Success(1)
    }

    "work when the value can be parsed as a positive integer" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_ALLOWED_FAILED_LOGINS_KEY,
          ConfigValueFactory.fromAnyRef("1")
        )
      SecurityService.tryAllowedFailedLogins(Configuration(config)) === Success(1)
    }

    "take the default value if the property is not set" in {
      val config = ConfigFactory.empty()
      SecurityService.tryAllowedFailedLogins(Configuration(config)) === Success(conf.AUTH_ALLOWED_FAILED_LOGINS_DEFAULT)
    }

    "fail if the value is a non-positive integer" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_ALLOWED_FAILED_LOGINS_KEY,
          ConfigValueFactory.fromAnyRef(-1)
        )
      SecurityService.tryAllowedFailedLogins(Configuration(config)).isFailure === true
    }

    "fail if the value is not an integer and cannot be parsed as one" in {
      val config = ConfigFactory
        .empty()
        .withValue(
          conf.AUTH_ALLOWED_FAILED_LOGINS_KEY,
          ConfigValueFactory.fromAnyRef(true)
        )
      SecurityService.tryAllowedFailedLogins(Configuration(config)).isFailure === true
    }

  }

}
