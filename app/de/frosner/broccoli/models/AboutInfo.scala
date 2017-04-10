package de.frosner.broccoli.models

import play.api.libs.json.{Json, Reads, Writes}

case class AboutInfo ( project: AboutProject
                     , scala: AboutScala
                     , sbt: AboutSbt
                     , auth: AboutAuth
                     , services: AboutServices
                     )

case class AboutProject(name: String, version: String)

case class AboutScala(version: String)

case class AboutSbt(version: String)

case class AboutAuth(enabled: Boolean, user: AboutUser)

case class AboutServices(clusterManager: AboutClusterManager, serviceDiscovery: AboutServiceDiscovery)

case class AboutUser(name: String, role: String, instanceRegex: String)

case class AboutClusterManager(connected: Boolean)

case class AboutServiceDiscovery(connected: Boolean)

object AboutInfo {

  private implicit val aboutServiceDiscoveryWrites = Json.writes[AboutServiceDiscovery]
  private implicit val aboutServiceDiscoveryReads = Json.reads[AboutServiceDiscovery]

  private implicit val aboutClusterManagerWrites = Json.writes[AboutClusterManager]
  private implicit val aboutClusterManagerReads = Json.reads[AboutClusterManager]

  private implicit val aboutUserWrites = Json.writes[AboutUser]
  private implicit val aboutUserReads = Json.reads[AboutUser]

  private implicit val aboutServicesWrites = Json.writes[AboutServices]
  private implicit val aboutServicesReads = Json.reads[AboutServices]

  private implicit val aboutAuthWrites = Json.writes[AboutAuth]
  private implicit val aboutAuthReads = Json.reads[AboutAuth]

  private implicit val aboutSbtWrites = Json.writes[AboutSbt]
  private implicit val aboutSbtReads = Json.reads[AboutSbt]

  private implicit val aboutScalaWrites = Json.writes[AboutScala]
  private implicit val aboutScalaReads = Json.reads[AboutScala]

  private implicit val aboutProjectWrites = Json.writes[AboutProject]
  private implicit val aboutProjectReads = Json.reads[AboutProject]


  implicit val aboutInfoWrites: Writes[AboutInfo] = Json.writes[AboutInfo]

  implicit val aboutInfoReads: Reads[AboutInfo] = Json.reads[AboutInfo]

}