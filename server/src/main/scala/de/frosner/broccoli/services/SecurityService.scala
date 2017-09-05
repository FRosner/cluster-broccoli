package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import cats.data.{EitherT, OptionT}
import cats.instances.future._
import cats.syntax.either._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{Account, AuthConfiguration, AuthMode}

import scala.concurrent.{ExecutionContext, Future}

sealed trait LoginError

object LoginError {
  final case object InvalidPassword extends LoginError
  final case object UnknownUser extends LoginError
  final case object Locked extends LoginError
}

@Singleton()
case class SecurityService @Inject()(
    configuration: AuthConfiguration,
    credentialsProvider: CredentialsProvider,
    identityService: IdentityService[Account]
)(implicit ec: ExecutionContext) {

  private val log = play.api.Logger(getClass)

  val sessionTimeoutInSeconds: Int = configuration.session.timeout.toSeconds.toInt

  val allowedFailedLogins: Int = configuration.allowedFailedLogins

  val authMode: AuthMode = configuration.mode

  val cookieSecure: Boolean = configuration.cookie.secure
  val allowMultiLogin: Boolean = configuration.session.allowMultiLogin

  @volatile
  private var failedLoginAttempts: Map[String, Int] = Map.empty

  /**
    * Authenticate some credentials.
    *
    * @param credentials The credentials to authenticate
    * @return The corresponding login information, or None if authentication failed.
    */
  def authenticate(credentials: Credentials): Future[Option[LoginInfo]] =
    EitherT
      .rightT(credentials)
      .ensure(LoginError.Locked)(c => failedLoginAttempts.getOrElse(c.identifier, 0) <= allowedFailedLogins)
      .flatMapF(credentialsProvider.authenticate(_).map(_.asRight).recover {
        case _: InvalidPasswordException  => LoginError.InvalidPassword.asLeft
        case _: IdentityNotFoundException => LoginError.UnknownUser.asLeft
      })
      .leftMap { error =>
        // Login if an account was locked
        val attempts = failedLoginAttempts.getOrElse(credentials.identifier, 0)
        if (error == LoginError.Locked) {
          log.warn(
            s"Credentials for '${credentials.identifier}' exceeded the allowed number of failed logins: " +
              s"$allowedFailedLogins (has $attempts)")
        }
        // Track the failed attempt
        failedLoginAttempts = failedLoginAttempts.updated(credentials.identifier, attempts + 1)
        error
      }
      .toOption
      .value
}
