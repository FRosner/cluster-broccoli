package de.frosner.broccoli.services

import com.google.common.collect.{ImmutableMap, Iterables, Lists, Maps}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import de.frosner.broccoli.conf
import de.frosner.broccoli.controllers.UserAccount
import org.specs2.mutable.Specification
import play.api.Configuration

import collection.JavaConverters._

class SecurityServiceSpec extends Specification {

  def configWithAccounts(accounts: Iterable[UserAccount]): Configuration = {
    val accountsJava = accounts.map { account =>
      ImmutableMap.of(
        conf.AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY, account.name,
        conf.AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY, account.password
      )
    }.asJava
    val config = ConfigFactory.empty().withValue(
      conf.AUTH_MODE_CONF_ACCOUNTS_KEY,
      ConfigValueFactory.fromIterable(accountsJava)
    )
    Configuration(config)
  }

  val account = UserAccount("frank", "pass")

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

}
