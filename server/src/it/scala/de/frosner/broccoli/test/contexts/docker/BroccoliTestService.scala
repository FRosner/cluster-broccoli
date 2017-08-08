package de.frosner.broccoli.test.contexts.docker

import enumeratum._

sealed trait BroccoliTestService extends EnumEntry {

  /**
    * The command to run for this service.
    */
  def command: Seq[String]
}

/**
  * Services that the Broccoli Test image provides
  */
object BroccoliTestService extends Enum[BroccoliTestService] {
  val values = findValues

  case object Broccoli extends BroccoliTestService {

    /**
      * The command to run for this service.
      */
    override def command: Seq[String] = Seq("cluster-broccoli")
  }

  case object Nomad extends BroccoliTestService {

    /**
      * The command to run for this service.
      */
    override def command: Seq[String] = Seq("nomad")
  }

  case object Consul extends BroccoliTestService {

    /**
      * The command to run for this service.
      */
    override def command: Seq[String] = Seq("consul")
  }
}
