package de.frosner.broccoli

import de.frosner.broccoli.models.UserAccount
import play.api.Configuration

package object conf {

  val NOMAD_URL_KEY = "broccoli.nomad.url"
  val NOMAD_URL_DEFAULT = "http://localhost:4646"

  val NOMAD_JOB_PREFIX_KEY = "broccoli.instances.prefix"
  val NOMAD_JOB_PREFIX_DEFAULT = ""
  def getNomadJobPrefix(configuration: Configuration): String =
    configuration.getString(conf.NOMAD_JOB_PREFIX_KEY).getOrElse(conf.NOMAD_JOB_PREFIX_DEFAULT)

  val CONSUL_URL_KEY = "broccoli.consul.url"
  val CONSUL_URL_DEFAULT = "http://localhost:8500"

  val TEMPLATES_STORAGE_TYPE_KEY = "broccoli.templates.storage.type"
  val TEMPLATES_STORAGE_TYPE_FILESYSTEM = "fs"
  val TEMPLATES_STORAGE_TYPE_DEFAULT = TEMPLATES_STORAGE_TYPE_FILESYSTEM

  val TEMPLATES_STORAGE_FS_URL_KEY = "broccoli.templates.storage.fs.url"
  val TEMPLATES_STORAGE_FS_URL_DEFAULT = "templates"

  val INSTANCES_STORAGE_TYPE_KEY = "broccoli.instances.storage.type"
  val INSTANCES_STORAGE_TYPE_FS = "fs"
  val INSTANCES_STORAGE_TYPE_COUCHDB = "couchdb"
  val INSTANCES_STORAGE_TYPE_DEFAULT = INSTANCES_STORAGE_TYPE_FS

  val INSTANCES_STORAGE_FS_URL_KEY = "broccoli.instances.storage.fs.url"
  val INSTANCES_STORAGE_FS_URL_DEFAULT = "instances"

  val INSTANCES_STORAGE_COUCHDB_URL_KEY = "broccoli.instances.storage.couchdb.url"
  val INSTANCES_STORAGE_COUCHDB_URL_DEFAULT = "http://localhost:5984"
  val INSTANCES_STORAGE_COUCHDB_DBNAME_KEY = "broccoli.instances.storage.couchdb.dbName"
  val INSTANCES_STORAGE_COUCHDB_DBNAME_DEFAULT = "broccoli_instances"

  val CONSUL_LOOKUP_METHOD_KEY = "broccoli.consul.lookup"
  val CONSUL_LOOKUP_METHOD_IP = "ip"
  val CONSUL_LOOKUP_METHOD_DNS = "dns"

  val POLLING_FREQUENCY_KEY = "broccoli.polling.frequency"
  val POLLING_FREQUENCY_DEFAULT = 1

  val AUTH_SESSION_TIMEOUT_KEY = "broccoli.auth.session.timeout"
  val AUTH_SESSION_TIMEOUT_DEFAULT = 3600

  val AUTH_MODE_KEY = "broccoli.auth.mode"
  val AUTH_MODE_NONE = "none"
  val AUTH_MODE_CONF = "conf"
  val AUTH_MODE_DEFAULT = AUTH_MODE_NONE

  val AUTH_MODE_CONF_ACCOUNTS_KEY = "broccoli.auth.conf.accounts"
  val AUTH_MODE_CONF_ACCOUNTS_DEFAULT = Set(UserAccount(name = "administrator", password = "broccoli")) // [{username:administrator,password:broccoli}]
  val AUTH_MODE_CONF_ACCOUNT_USERNAME_KEY = "username"
  val AUTH_MODE_CONF_ACCOUNT_PASSWORD_KEY = "password"

  val PERMISSIONS_MODE_KEY = "broccoli.permissions.mode"
  val PERMISSIONS_MODE_ADMINISTRATOR = "administrator"
  val PERMISSIONS_MODE_OPERATOR = "operator"
  val PERMISSIONS_MODE_USER = "user"
  val PERMISSIONS_MODE_DEFAULT = PERMISSIONS_MODE_ADMINISTRATOR

}
