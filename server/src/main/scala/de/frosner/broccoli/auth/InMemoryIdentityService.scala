package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import scala.concurrent.Future

/**
  * An in-memory identity service.
  *
  * @param identities The known identities
  */
class InMemoryIdentityService(identities: Seq[Account]) extends IdentityService[Account] {
  val logins: Map[String, Account] = identities.map(account => account.name -> account).toMap

  /**
    * Find a user in the list of identities.
    *
    * @param loginInfo The login information
    * @return The identity if any
    */
  override def retrieve(loginInfo: LoginInfo): Future[Option[Account]] =
    Future.successful(logins.get(loginInfo.providerKey))
}
