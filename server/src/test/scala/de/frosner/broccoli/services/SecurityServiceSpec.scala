package de.frosner.broccoli.services

import cats.data.OptionT
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.exceptions.InvalidPasswordException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{Account, AuthConfiguration, AuthMode, Role}
import org.mockito.Mock
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class SecurityServiceSpec extends Specification with Mockito with ExecutionEnvironment {

  def configWithAccounts(accounts: Seq[Account]): AuthConfiguration =
    AuthConfiguration(
      mode = AuthMode.Conf,
      session = AuthConfiguration.Session(timeout = Duration(1, "hour"), allowMultiLogin = true),
      cookie = AuthConfiguration.Cookie(secure = true),
      conf = AuthConfiguration.Conf(
        accounts = accounts
          .map(
            a =>
              AuthConfiguration.ConfAccount(
                a.name,
                "",
                a.instanceRegex,
                a.role
            ))
          .toList),
      allowedFailedLogins = 3
    )

  val identityService = mock[IdentityService[Account]]

  val account = Account("frank", "^test.*", Role.Administrator)

  override def is(implicit executionEnv: ExecutionEnv): Any =
    "An authentication check" should {

      "succeed if the credentials provider authenticates" in {
        val login = LoginInfo(CredentialsProvider.ID, account.name)
        val credentials = Credentials(account.name, "pass")

        val credentialsProvider = mock[CredentialsProvider]
        credentialsProvider.authenticate(credentials) returns Future.successful(login)

        SecurityService(configWithAccounts(List(account)), credentialsProvider, identityService)
          .authenticate(credentials) must beSome(login).await
      }

      "fail if the credentials provider fails to authenticate" in {
        val credentials = Credentials(account.name, "pass")

        val credentialsProvider = mock[CredentialsProvider]
        credentialsProvider.authenticate(credentials) returns Future.failed(new InvalidPasswordException("foo"))

        SecurityService(configWithAccounts(List(account)), credentialsProvider, identityService)
          .authenticate(credentials) must beNone.await
      }

      "succeed if the number of failed logins is equal to the allowed ones" in {
        val credentials = Credentials(account.name, "pass")
        val failedCredentials = credentials.copy(password = "foo")
        val login = LoginInfo(CredentialsProvider.ID, credentials.identifier)

        val credentialsProvider = mock[CredentialsProvider]
        credentialsProvider.authenticate(failedCredentials) returns Future.failed(new InvalidPasswordException("foo"))
        credentialsProvider.authenticate(credentials) returns Future.successful(login)

        val service = SecurityService(configWithAccounts(List(account)), credentialsProvider, identityService)
        val failedAttempts = for (attemptNo <- 1 to service.allowedFailedLogins) {
          service.authenticate(failedCredentials) must beNone.await
        }
        service.authenticate(credentials) must beSome(login).await
      }

      "fail if the number of failed logins is greater than the allowed number" in {
        val credentials = Credentials(account.name, "password")
        val failedCredentials = credentials.copy(password = "foo")
        val login = LoginInfo(CredentialsProvider.ID, credentials.identifier)

        val credentialsProvider = mock[CredentialsProvider]
        credentialsProvider.authenticate(failedCredentials) returns Future.failed(new InvalidPasswordException("foo"))
        credentialsProvider.authenticate(credentials) returns Future.successful(login)

        val service = SecurityService(configWithAccounts(List(account)), credentialsProvider, identityService)
        val failedAttempts = for (attemptNo <- 0 to service.allowedFailedLogins) {
          service.authenticate(failedCredentials) must beNone.await
        }
        service.authenticate(credentials) must beNone.await
      }

    }
}
