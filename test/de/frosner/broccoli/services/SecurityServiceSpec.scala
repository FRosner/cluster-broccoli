package de.frosner.broccoli.services

import com.google.common.collect.{ImmutableMap, Iterables, Lists, Maps}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Role, UserAccount}
import org.specs2.mutable.Specification
import play.api.Configuration

import collection.JavaConverters._
import scala.util.Success

class SecurityServiceSpec extends Specification {

  def configWithAccounts(accounts: Iterable[UserAccount]): Configuration = {
    val accountsJava = accounts.map { account =>
      ImmutableMap.of(
        conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
        conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password,
        conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, account.role.toString,
        conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, account.instanceRegex
      )
    }.asJava
    val config = ConfigFactory.empty().withValue(
      conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
      ConfigValueFactory.fromIterable(accountsJava)
    )
    Configuration(config)
  }

  val account = UserAccount("frank", "pass", "^test.*", Role.Administrator)

  "An authentication check" should {

    "succeed if the account matches" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(account) === true
    }

    "fail if the username does not exist" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(account.copy(name = "new")) === false
    }

    "fail if the password does not matche" in {
      SecurityService(configWithAccounts(List(account)))
        .isAllowedToAuthenticate(account.copy(password = "new")) === false
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
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, account.instanceRegex,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, account.role.toString
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)) === Success(Iterable(account))
    }

    "not require the optional parameters" in {
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)) === Success(Iterable(UserAccount(
        name = account.name,
        password = account.password,
        instanceRegex = conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_DEFAULT,
        role = conf.AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT
      )))
    }

    "fail if the accounts are not a config list" in {
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromAnyRef("blub")
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

    "fail if each accounts element is not a config object" in {
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromAnyRef("blub")
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

    "if the username is not a string" in {
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, 5,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, account.role.toString,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, account.instanceRegex
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

    "if the password is not a string" in {
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, 5,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, account.role.toString,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, account.instanceRegex
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

    "if the instance regex is not a string" in {
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, account.role.toString,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, 5
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

    "if the role is not a string" in {
      val accountsJava = Iterable{
        ImmutableMap.of(
          conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
          conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password,
          conf.AUTH_MODE_CONF_ACCOUNT_ROLE_KEY, 5,
          conf.AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY, account.instanceRegex
        )
      }.asJava
      val config = ConfigFactory.empty().withValue(
        conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
        ConfigValueFactory.fromIterable(accountsJava)
      )
      SecurityService.tryAccounts(Configuration(config)).failed.get should beAnInstanceOf[Exception]
    }

  }

}
