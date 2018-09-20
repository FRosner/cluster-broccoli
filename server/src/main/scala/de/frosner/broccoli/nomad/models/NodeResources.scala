package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

object NodeResources {
  implicit val nodeResourcesWrites: Writes[NodeResources] = Json.writes[NodeResources]
}

case class NodeResources(nodeId: String,
                         nodeName: String,
                         totalResources: TotalResources,
                         hostResources: HostResources,
                         allocatedResources: Map[String, AllocatedResources],
                         allocatedResourceUtilization: Map[String, AllocatedResourcesUtlization])

/**
  * @param id allocation id
  * @param name allocation name
  * @param cpu CPU in MHz
  * @param memoryMB Memory in MB
  * @param diskMB Disk in MB
  */
case class AllocatedResources(id: String, name: String, cpu: Long, memoryMB: Long, diskMB: Long)

/**
  * @param id allocation id
  * @param name allocation name
  * @param cpu CPU in MHz
  * @param memoryMB Memory in MB
  */
case class AllocatedResourcesUtlization(id: String, name: String, cpu: Long, memoryMB: Long)

/**
  * @param cpu CPU in MHz
  * @param memoryUsed in bytes
  * @param memoryTotal in bytes
  * @param diskUsed in bytes
  * @param diskSize in bytes
  */
case class HostResources(cpu: Long, memoryUsed: Long, memoryTotal: Long, diskUsed: Long, diskSize: Long)

/**
  * @param cpu in MHz
  * @param memoryMB in MB
  * @param diskMB in MB
  */
case class TotalResources(cpu: Long, memoryMB: Long, diskMB: Long)