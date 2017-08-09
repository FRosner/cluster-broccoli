package de.frosner.broccoli.services

import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.models._
import org.specs2.mutable.Specification
import org.mockito.Mockito._

class AboutInfoServiceSpec extends Specification with ServiceMocks {

  "Requesting about info" should {

    "return the correct about info" in {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val service = new AboutInfoService(
        nomadService = withNomadReachable(mock(classOf[NomadService])),
        consulService = withConsulReachable(mock(classOf[ConsulService])),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )
      service.aboutInfo(account) === AboutInfo(
        project = AboutProject(
          name = de.frosner.broccoli.build.BuildInfo.name,
          version = de.frosner.broccoli.build.BuildInfo.version
        ),
        scala = AboutScala(
          version = de.frosner.broccoli.build.BuildInfo.scalaVersion
        ),
        sbt = AboutSbt(
          version = de.frosner.broccoli.build.BuildInfo.sbtVersion
        ),
        auth = AboutAuth(
          enabled = false,
          user = AboutUser(
            name = account.name,
            role = account.role,
            instanceRegex = account.instanceRegex
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
    }

  }

}
