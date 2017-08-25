package de.frosner.broccoli.auth

import de.frosner.broccoli.RemoveSecrets
import enumeratum._

import scala.collection.immutable

/**
  * A user role in the application
  */
sealed trait UserRole extends EnumEntry with EnumEntry.Lowercase

object UserRole extends Enum[UserRole] with PlayJsonEnum[UserRole] {
  override def values: immutable.IndexedSeq[UserRole] = findValues

  /**
    * Grants all privileges.
    */
  final case object Administrator extends UserRole

  /**
    * Grants limited privileges to edit instances.
    */
  final case object Operator extends UserRole

  /**
    * Grants use of instances but does not allow modification.
    */
  final case object User extends UserRole

  /**
    * Filter secrets unless a user has the Administrator role.
    *
    * Only administrators are allowed to view secrets in Broccoli so this function removes secrets from values unless
    * a user is administrator.
    *
    * @param role The role of the user
    * @param value The value to filter
    * @param removeSecrets The RemoveSecrets instance for the value type
    * @tparam T The value type
    * @return The value filtered according to RemoveSecrets
    */
  def removeSecretsForRole[T](role: UserRole)(value: T)(implicit removeSecrets: RemoveSecrets[T]): T = role match {
    case UserRole.Administrator => value
    case _                      => removeSecrets.removeSecrets(value)
  }

  /**
    * Provide additional syntax concerned with roles.
    */
  trait RoleSyntax {

    implicit class RemoveSecretsForRoleOps[T](value: T)(implicit removeSecrets: RemoveSecrets[T]) {

      /**
        * Remove secrets from this value according to a user role.
        *
        * @param role The user role
        * @return The value with secrets removed if the role requires it, otherwise this value
        */
      def removeSecretsForRole(role: UserRole): T = UserRole.removeSecretsForRole(role)(value)(removeSecrets)
    }
  }

  object syntax extends RoleSyntax
}
