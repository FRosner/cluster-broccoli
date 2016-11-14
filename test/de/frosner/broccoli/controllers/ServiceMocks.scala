package de.frosner.broccoli.controllers

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.services._
import org.mockito.Mockito._
import org.mockito.internal.util.MockUtil

trait ServiceMocks {

  private val mockUtil = new MockUtil()
  private def requireMock(obj: AnyRef): Unit = require(
    mockUtil.isMock(obj),
    s"${obj.getClass.toString} needs to be a mock!"
  )

  def withAuthConf(securityService: SecurityService, allowed: Iterable[Account]): SecurityService = {
    requireMock(securityService)
    allowed.foreach { account =>
      when(securityService.getAccount(account.name)).thenReturn(Some(account))
      when(securityService.isAllowedToAuthenticate(account)).thenReturn(true)
    }
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_CONF)
    securityService
  }

  def withAuthNone(securityService: SecurityService): SecurityService = {
    requireMock(securityService)
    when(securityService.authMode).thenReturn(conf.AUTH_MODE_NONE)
    securityService
  }

  def withDummyValues(buildInfoService: BuildInfoService): BuildInfoService = {
    requireMock(buildInfoService)
    when(buildInfoService.projectName).thenReturn("project")
    when(buildInfoService.projectVersion).thenReturn("version")
    when(buildInfoService.scalaVersion).thenReturn("scala")
    when(buildInfoService.sbtVersion).thenReturn("sbt")
    buildInfoService
  }

  def withEmptyInstancePrefix(instanceService: InstanceService): InstanceService = {
    requireMock(instanceService)
    when(instanceService.nomadJobPrefix).thenReturn("")
    instanceService
  }

  def withDefaultPermissionsMode(permissionsService: PermissionsService): PermissionsService = {
    requireMock(permissionsService)
    when(permissionsService.getPermissionsMode()).thenReturn(conf.PERMISSIONS_MODE_DEFAULT)
    permissionsService
  }

  def withTemplates(templateService: TemplateService, templates: Seq[Template]): TemplateService = {
    when(templateService.getTemplates).thenReturn(templates)
    templates.foreach { template =>
      when(templateService.template(template.id)).thenReturn(Some(template))
    }
    templateService
  }

}