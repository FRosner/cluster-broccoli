package de.frosner.broccoli.auth

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.impl.authenticators.{
  CookieAuthenticator,
  CookieAuthenticatorService,
  CookieAuthenticatorSettings
}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.InMemoryAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings}
import de.frosner.broccoli.BroccoliConfiguration
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, PlayCacheLayer, SecureRandomIDGenerator}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import play.api.mvc.{Cookie, CookieHeaderEncoding}
import com.typesafe.config.Config
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Configure authentication for Broccoli.
  */
class AuthModule @Inject() extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[PasswordHasherRegistry].toInstance(PasswordHasherRegistry(new BCryptPasswordHasher()))
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  /**
    * A very nested optional reader, to support these cases:
    * Not set, set None, will use default ('Lax')
    * Set to null, set Some(None), will use 'No Restriction'
    * Set to a string value try to match, Some(Option(string))
    */
  implicit val sameSiteReader: ValueReader[Option[Option[Cookie.SameSite]]] =
    (config: Config, path: String) => {
      if (config.hasPathOrNull(path)) {
        if (config.getIsNull(path))
          Some(None)
        else {
          Some(Cookie.SameSite.parse(config.getString(path)))
        }
      } else {
        None
      }
    }

  /**
    * Provides authentication configuration.
    *
    * @param config The Broccoli configuration
    * @return The authentication configuration of config
    */
  @Provides
  def providesAuthConfiguration(config: BroccoliConfiguration): AuthConfiguration = config.auth

  /**
    * Provides the identity service for Broccoli.
    *
    * @param config The configuration
    * @return The identity service
    */
  @Provides
  def providesIdentityService(config: AuthConfiguration): IdentityService[Account] = {
    val accounts = config.mode match {
      case AuthMode.None => Seq(Account.anonymous)
      case AuthMode.Conf =>
        config.conf.accounts.map(
          account => Account(account.username, account.instanceRegex, account.role)
        )
    }
    new InMemoryIdentityService(accounts)
  }

  /**
    * Provides the Silhouette environment.
    *
    * @param userService The user service implementation.
    * @param authenticatorService The authentication service implementation.
    * @param eventBus The event bus instance.
    * @return The Silhouette environment.
    */
  @Provides
  def provideEnvironment(userService: IdentityService[Account],
                         authenticatorService: AuthenticatorService[CookieAuthenticator],
                         eventBus: EventBus): Environment[DefaultEnv] =
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )

  /**
    * Provides the signer for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The signer for the authenticator.
    */
  @Provides @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.authenticator.signer")
    new JcaSigner(config)
  }

  /**
    * Provides the crypter for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the authenticator.
    */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    new JcaCrypter(config)
  }

  /**
    * Provides the authentication info repository.
    *
    * If authentication mode is conf create an in-memory DAO for password info and add all accounts from configuration
    * to it.  Hash all passwords with bcrypt to slow-down login and make password-cracking attacks less feasible.
    *
    * @param config The authentication configuration
    * @param passwordHasherRegistry The current and deprecated password hashers.
    * @return An authentication info repository.
    */
  @Provides
  @Singleton
  def providesAuthInfoRepository(
      config: AuthConfiguration,
      passwordHasherRegistry: PasswordHasherRegistry
  ): AuthInfoRepository =
    new DelegableAuthInfoRepository(config.mode match {
      case AuthMode.Conf =>
        // Create an in-memory DAO and add all accounts from configuration to it
        val dao = new InMemoryAuthInfoDAO[PasswordInfo]()
        config.conf.accounts.foreach { account =>
          dao.add(LoginInfo(CredentialsProvider.ID, account.username),
                  passwordHasherRegistry.current.hash(account.password))
        }
        dao
      case _ =>
        // Use an empty DAO
        new InMemoryAuthInfoDAO[PasswordInfo]()
    })

  /**
    * Provides the authenticator service.
    *
    * @param signer The signer implementation.
    * @param crypter The crypter implementation.
    * @param cookieHeaderEncoding Logic for encoding and decoding `Cookie` and `Set-Cookie` headers.
    * @param fingerprintGenerator The fingerprint generator implementation.
    * @param idGenerator The ID generator implementation.
    * @param configuration The Play configuration.
    * @param clock The clock instance.
    * @return The authenticator service.
    */
  @Provides
  def provideAuthenticatorService(@Named("authenticator-signer") signer: Signer,
                                  @Named("authenticator-crypter") crypter: Crypter,
                                  cookieHeaderEncoding: CookieHeaderEncoding,
                                  fingerprintGenerator: FingerprintGenerator,
                                  idGenerator: IDGenerator,
                                  configuration: Configuration,
                                  clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val authenticatorEncoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config,
                                   None,
                                   signer,
                                   cookieHeaderEncoding,
                                   authenticatorEncoder,
                                   fingerprintGenerator,
                                   idGenerator,
                                   clock)
  }

  /**
    * Provides the credentials provider.
    *
    * @param authInfoRepository The auth info repository implementation.
    * @param passwordHasherRegistry The password hasher registry.
    * @return The credentials provider.
    */
  @Provides
  def provideCredentialsProvider(authInfoRepository: AuthInfoRepository,
                                 passwordHasherRegistry: PasswordHasherRegistry): CredentialsProvider =
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
}
