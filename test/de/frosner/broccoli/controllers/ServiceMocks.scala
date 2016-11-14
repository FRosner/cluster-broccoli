package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.services.{BuildInfoService, InstanceService, PermissionsService, SecurityService}
import org.mockito.Mockito._

trait ServiceMocks {

  def withAuthConf(securityService: SecurityService, allowed: Iterable[Account]): SecurityService = {
    allowed.foreach { account =>
      when(securityService.getAccount(account.name)).thenReturn(Some(account))
      when(securityService.isAllowedToAuthenticate(account)).thenReturn(true)
    }
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_CONF)
    securityService
  }

  def withAuthNone(securityService: SecurityService): SecurityService = {
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_NONE)
    securityService
  }

  def withDummyValues(buildInfoService: BuildInfoService): BuildInfoService = {
    when(buildInfoService.projectName).thenReturn("project")
    when(buildInfoService.projectVersion).thenReturn("version")
    when(buildInfoService.scalaVersion).thenReturn("scala")
    when(buildInfoService.sbtVersion).thenReturn("sbt")
    buildInfoService
  }

  def withEmptyInstancePrefix(instanceService: InstanceService): InstanceService = {
    when(instanceService.nomadJobPrefix).thenReturn("")
    instanceService
  }

  def withDefaultPermissionsMode(permissionsService: PermissionsService): PermissionsService = {
    when(permissionsService.getPermissionsMode()).thenReturn(conf.PERMISSIONS_MODE_DEFAULT)
    permissionsService
  }

}