package de.frosner.broccoli.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import de.frosner.broccoli.auth.{Account, AuthMode, DefaultEnv, Role}
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad.NomadClient
import de.frosner.broccoli.nomad.models.NodeResources
import de.frosner.broccoli.services._
import org.mockito.Matchers
import org.mockito.internal.util.MockUtil
import org.specs2.mock.Mockito
import play.api.libs.json.{JsString, JsValue}
import play.api.mvc
import play.api.mvc.PlayBodyParsers
import play.api.test.Helpers._
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

trait ServiceMocks extends Mockito {

  implicit val actorSystem = ActorSystem("ServiceMocks")
  implicit val materializer = ActorMaterializer()

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

  def withNomadAndConsulReachable(instanceService: InstanceService): InstanceService = {
    requireMock(instanceService)
    instanceService.isNomadReachable returns true
    instanceService.isConsulReachable returns true
    instanceService
  }

  def withTemplates(templateService: TemplateService, templates: Seq[Template]): TemplateService = {
    requireMock(templateService)
    templateService.getTemplates returns templates
    templates.foreach { template =>
      templateService.template(template.id) returns Some(template)
    }
    templateService
  }

  def withParseHcl(nomadService: NomadService, hclJob: String, response: JsValue): NomadService = {
    requireMock(nomadService)
    when(nomadService.parseHCLJob(hclJob)).thenReturn(Success(response))
    when(nomadService.startJob(JsString(hclJob))).thenReturn(Success(()))
    nomadService
  }

  def withNomadVersion(nomadClient: NomadClient, version: String): NomadClient = {
    requireMock(nomadClient)
    when(nomadClient.nomadVersion).thenReturn(version)
    nomadClient
  }

  def withInstances(instanceService: InstanceService, instances: Seq[InstanceWithStatus]): InstanceService = {
    requireMock(instanceService)
    instanceService.getInstances returns instances
    instances.foreach { instance =>
      instanceService.getInstance(instance.instance.id) returns Some(instance)
    }
    instanceService
  }

  def withNodesResources(nomadService: NomadService, nodesResources: Seq[NodeResources]): NomadService = {
    requireMock(nomadService)
    nomadService.getNodeResources(any[Account]) returns Future.fromTry(Try(nodesResources))
    nomadService
  }

  def withIdentities(users: DefaultEnv#I*): Silhouette[DefaultEnv] = {
    val environment = FakeEnvironment[DefaultEnv](users.map(user => LoginInfo(user.name, user.name) -> user))
    val defaultParser = new mvc.BodyParsers.Default(PlayBodyParsers())
    val securedAction = new DefaultSecuredAction(
      new DefaultSecuredRequestHandler(new DefaultSecuredErrorHandler(stubMessagesApi())),
      defaultParser)
    val unsecuredAction = new DefaultUnsecuredAction(
      new DefaultUnsecuredRequestHandler(new DefaultUnsecuredErrorHandler(stubMessagesApi())),
      defaultParser)
    val userAware = new DefaultUserAwareAction(new DefaultUserAwareRequestHandler(), defaultParser)
    new SilhouetteProvider[DefaultEnv](environment, securedAction, unsecuredAction, userAware)
  }

  def withEnvironment(users: DefaultEnv#I*): Environment[DefaultEnv] =
    FakeEnvironment[DefaultEnv](users.map(user => LoginInfo(user.name, user.name) -> user))
}
