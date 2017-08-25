package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.util.{PasswordHasher, PasswordInfo}

/**
  * A password hasher that doesn't actually hash passwords.
  *
  * For use with conf auth mode where we store all passwords in a
  */
object NoPasswordHasher extends PasswordHasher {

  /**
    * The ID of this password hasher.
    */
  override val id = "nohash"

  /**
    * @return The given password again
    */
  override def hash(plainPassword: String): PasswordInfo = PasswordInfo(id, plainPassword, None)

  /**
    * Check whether the password in passwordInfo matches the given password.
    */
  override def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean =
    passwordInfo.password == suppliedPassword

  override def isDeprecated(passwordInfo: PasswordInfo): Option[Boolean] =
    if (isSuitable(passwordInfo)) Some(false) else None
}
