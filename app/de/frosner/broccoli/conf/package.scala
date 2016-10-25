package de.frosner.broccoli

package object conf {

  val NOMAD_URL_KEY = "broccoli.nomad.url"
  val NOMAD_URL_DEFAULT = "http://localhost:4646"

  val NOMAD_JOB_PREFIX_KEY = "broccoli.nomad.jobPrefix"
  val NOMAD_JOB_PREFIX_DEFAULT = ""

  val CONSUL_URL_KEY = "broccoli.consul.url"
  val CONSUL_URL_DEFAULT = "http://localhost:8500"

  val TEMPLATES_DIR_KEY = "broccoli.templatesDir"
  val TEMPLATES_DIR_DEFAULT = "templates"

  val INSTANCES_FILE_KEY = "broccoli.instancesFile"
  val INSTANCES_FILE_DEFAULT = "instances"

  val CONSUL_LOOKUP_METHOD_KEY = "broccoli.consul.lookup"
  val CONSUL_LOOKUP_METHOD_IP = "ip"
  val CONSUL_LOOKUP_METHOD_DNS = "dns"

  val POLLING_FREQUENCY_KEY = "broccoli.polling.frequency"
  val POLLING_FREQUENCY_DEFAULT = 1

  val PERMISSIONS_MODE_KEY = "broccoli.permissions.mode"
  val PERMISSIONS_MODE_ADMINISTRATOR = "administrator"
  val PERMISSIONS_MODE_OPERATOR = "operator"
  val PERMISSIONS_MODE_USER = "user"
  val PERMISSIONS_MODE_DEFAULT = PERMISSIONS_MODE_ADMINISTRATOR

}
