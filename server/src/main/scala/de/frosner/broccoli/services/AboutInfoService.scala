package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models._

@Singleton
class AboutInfoService @Inject()(nomadService: NomadService,
                                 consulService: ConsulService,
                                 securityService: SecurityService) {

  def aboutInfo(loggedIn: Account) = AboutInfo(
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
      enabled = securityService.authMode != conf.AUTH_MODE_NONE,
      user = AboutUser(
        name = loggedIn.name,
        role = loggedIn.role,
        instanceRegex = loggedIn.instanceRegex
      )
    ),
    services = AboutServices(
      clusterManager = AboutClusterManager(connected = nomadService.isNomadReachable),
      serviceDiscovery = AboutServiceDiscovery(connected = consulService.isConsulReachable)
    )
  )

}
