package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.Future

/**
  * A password info DAO with a constant list of known logins.
  *
  * @param knownPasswords The known passwords
  */
class ConstantPasswordInfoDAO(knownPasswords: Map[String, PasswordInfo]) extends DelegableAuthInfoDAO[PasswordInfo] {
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    Future.successful(knownPasswords.get(loginInfo.providerKey))

  /**
    * Not supported.
    *
    * Fails the future with UnsupportedOperationException.
    */
  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    Future.failed(new UnsupportedOperationException(s"Cannot add logins to constant list of logins"))

  /**
    * Not supported.
    *
    * Fails the future with UnsupportedOperationException.
    */
  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    Future.failed(new UnsupportedOperationException(s"Cannot update logins in constant list of logins"))

  /**
    * Not supported.
    *
    * Fails the future with UnsupportedOperationException.
    */
  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    Future.failed(new UnsupportedOperationException(s"Cannot save logins in constant list of logins"))

  /**
    * Not supported.
    *
    * Fails the future with UnsupportedOperationException.
    */
  override def remove(loginInfo: LoginInfo): Future[Unit] =
    Future.failed(new UnsupportedOperationException(s"Cannot remove logins from constant list of logins"))

}
