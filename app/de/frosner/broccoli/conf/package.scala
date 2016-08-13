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

}
