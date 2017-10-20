package de.frosner.broccoli.controllers

import cats.data.OptionT
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import de.frosner.broccoli.auth.{Account, AuthMode, Role}
import de.frosner.broccoli.models._
import de.frosner.broccoli.services._
import org.mockito.Matchers
import org.mockito.internal.util.MockUtil
import org.specs2.mock.Mockito

import scala.concurrent.Future

trait ServiceMocks extends Mockito {

  private val mockUtil = new MockUtil()
  private def requireMock(obj: AnyRef): Unit = require(
    mockUtil.isMock(obj),
    s"${obj.getClass.toString} needs to be a mock!"
  )

  def withAuthConf(securityService: SecurityService, allowed: Iterable[Account]): SecurityService = {
    requireMock(securityService)
    val identityService = mock[IdentityService[Account]]
    allowed.foreach { account =>
      identityService.retrieve(LoginInfo(CredentialsProvider.ID, account.name)) returns Future.successful(Some(account))
      securityService.identityService returns identityService
      securityService.authenticate(Credentials(account.name, "password")) returns Future.successful(
        Some(LoginInfo(CredentialsProvider.ID, account.name)))
    }
    securityService.authMode returns AuthMode.Conf
    securityService
  }

  def withAuthNone(securityService: SecurityService): SecurityService = {
    requireMock(securityService)
    securityService.authMode returns AuthMode.None
    securityService
  }

  def withDummyValues(aboutInfoService: AboutInfoService): AboutInfoService = {
    requireMock(aboutInfoService)
    aboutInfoService.aboutInfo(Matchers.any(classOf[Account])) returns AboutInfo(
      project = AboutProject(
        name = "project",
        version = "version"
      ),
      scala = AboutScala(
        version = "scala"
      ),
      sbt = AboutSbt(
        version = "sbt"
      ),
      auth = AboutAuth(
        enabled = false,
        user = AboutUser(
          name = "name",
          role = Role.User,
          instanceRegex = "instances"
        )
      ),
      services = AboutServices(
        clusterManager = AboutClusterManager(
          connected = true
        ),
        serviceDiscovery = AboutServiceDiscovery(
          connected = true
        )
      )
    )
    aboutInfoService
  }

  def withConsulReachable(consulService: ConsulService): ConsulService = {
    requireMock(consulService)
    consulService.isConsulReachable returns true
    consulService
  }

  def withNomadReachable(nomadService: NomadService): NomadService = {
    requireMock(nomadService)
    nomadService.isNomadReachable returns true
    nomadService
  }

  def withTemplates(templateService: TemplateService, templates: Seq[Template]): TemplateService = {
    requireMock(templateService)
    templateService.getTemplates returns templates
    templates.foreach { template =>
      templateService.template(template.id) returns Some(template)
    }
    templateService
  }

  def withInstances(instanceService: InstanceService, instances: Seq[InstanceWithStatus]): InstanceService = {
    requireMock(instanceService)
    instanceService.getInstances returns instances
    instances.foreach { instance =>
      instanceService.getInstance(instance.instance.id) returns Some(instance)
    }
    instanceService
  }

}
