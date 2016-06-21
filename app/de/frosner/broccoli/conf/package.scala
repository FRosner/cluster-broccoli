package de.frosner.broccoli

package object conf {

  val NOMAD_URL_KEY = "broccoli.nomad.url"
  val NOMAD_URL_DEFAULT = "http://localhost:4646"

  val NOMAD_JOB_PREFIX_KEY = "broccoli.nomad.jobPrefix"
  val NOMAD_JOB_PREFIX_DEFAULT = ""

  val CONSUL_URL_KEY = "broccoli.consul.url"
  val CONSUL_URL_DEFAULT = "http://localhost:8500"

}
