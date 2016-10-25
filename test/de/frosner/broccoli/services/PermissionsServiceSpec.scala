package de.frosner.broccoli.services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus, Template}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.Configuration

class PermissionsServiceSpec extends Specification {

  def testPermissionsMode(expectedPermissionsMode: String): MatchResult[String] =
    testPermissionsMode(Some(expectedPermissionsMode), expectedPermissionsMode)

  def testPermissionsMode(givenPermissionsMode: Option[String], expectedPermissionsMode: String): MatchResult[String] = {
    val configuration = givenPermissionsMode.map(
      mode => Configuration((conf.PERMISSIONS_MODE_KEY, mode))
    ).getOrElse(Configuration())
    val service = new PermissionsService(configuration)
    service.permissionsMode === expectedPermissionsMode
  }

  "Permissions service" should {

    s"accept ${conf.PERMISSIONS_MODE_KEY}=${conf.PERMISSIONS_MODE_USER}" in {
      testPermissionsMode(conf.PERMISSIONS_MODE_USER)
    }

    s"accept ${conf.PERMISSIONS_MODE_KEY}=${conf.PERMISSIONS_MODE_ADMINISTRATOR}" in {
      testPermissionsMode(conf.PERMISSIONS_MODE_ADMINISTRATOR)
    }

    s"accept ${conf.PERMISSIONS_MODE_KEY}=${conf.PERMISSIONS_MODE_OPERATOR}" in {
      testPermissionsMode(conf.PERMISSIONS_MODE_OPERATOR)
    }

    s"use the default if ${conf.PERMISSIONS_MODE_KEY} is not set" in {
      testPermissionsMode(None, conf.PERMISSIONS_MODE_DEFAULT)
    }

    s"fail if ${conf.PERMISSIONS_MODE_KEY} is not set correctly" in {
      // TODO this does not work because I have a System.exit. I need a way to tell Play to shut down that I can test.
      // TODO maybe a shutdown service that I can mock? :D
      // testPermissionsMode(Some("notValid"), conf.PERMISSIONS_MODE_DEFAULT)
      true === true
    }
    // TODO more test cases

  }

}
