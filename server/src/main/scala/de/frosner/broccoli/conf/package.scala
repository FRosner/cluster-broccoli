package de.frosner.broccoli

import de.frosner.broccoli.auth.UserAccount
import de.frosner.broccoli.models.Role
import play.api.Configuration

package object conf {

  val CONSUL_URL_KEY = "broccoli.consul.url"
  val CONSUL_URL_DEFAULT = "http://localhost:8500"

  val CONSUL_LOOKUP_METHOD_KEY = "broccoli.consul.lookup"
  val CONSUL_LOOKUP_METHOD_IP = "ip"
  val CONSUL_LOOKUP_METHOD_DNS = "dns"

  val AUTH_SESSION_TIMEOUT_KEY = "broccoli.auth.session.timeout"
  val AUTH_SESSION_TIMEOUT_DEFAULT = 3600

  val AUTH_SESSION_ALLOW_MULTI_LOGIN_KEY = "broccoli.auth.session.allowMultiLogin"
  val AUTH_SESSION_ALLOW_MULTI_LOGIN_DEFAULT = true

  val AUTH_COOKIE_SECURE_KEY = "broccoli.auth.cookie.secure"
  val AUTH_COOKIE_SECURE_DEFAULT = true

  val AUTH_ALLOWED_FAILED_LOGINS_KEY = "broccoli.auth.allowedFailedLogins"
  val AUTH_ALLOWED_FAILED_LOGINS_DEFAULT = 3

  val AUTH_MODE_KEY = "broccoli.auth.mode"
  val AUTH_MODE_NONE = "none"
  val AUTH_MODE_CONF = "conf"
  val AUTH_MODE_DEFAULT = AUTH_MODE_NONE

  val AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY = "username"
  val AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY = "password"
  val AUTH_MODE_CONF_ACCOUNT_INSTANCEREGEX_KEY = "instanceRegex"
  val AUTH_MODE_CONF_ACCOUNT_ROLE_KEY = "role"
  val AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT = Role.Administrator

  val AUTH_MODE_CONF_ACCOUNTS_KEY = "broccoli.auth.conf.accounts"
  val AUTH_MODE_CONF_ACCOUNTS_DEFAULT = Iterable(
    UserAccount(
      name = "administrator",
      password = "broccoli",
      instanceRegex = ".*",
      role = AUTH_MODE_CONF_ACCOUNT_ROLE_DEFAULT
    ))

  val PERMISSIONS_MODE_KEY = "broccoli.permissions.mode"
  val PERMISSIONS_MODE_ADMINISTRATOR = "administrator"
  val PERMISSIONS_MODE_OPERATOR = "operator"
  val PERMISSIONS_MODE_USER = "user"
  val PERMISSIONS_MODE_DEFAULT = PERMISSIONS_MODE_ADMINISTRATOR

}
