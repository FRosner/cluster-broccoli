package de.frosner.broccoli.nomad.models

import play.api.libs.json.{Json, Writes}

object NodeResources {
  implicit val totalResourcesWrites: Writes[TotalResources] = Json.writes[TotalResources]
  implicit val totalUtilizationWrites: Writes[TotalUtilization] = Json.writes[TotalUtilization]
  implicit val hostResources: Writes[HostResources] = Json.writes[HostResources]
  implicit val allocatedResourcesWrites: Writes[AllocatedResources] = Json.writes[AllocatedResources]
  implicit val allocatedResourcesUtilizationWrites: Writes[AllocatedResourcesUtilization] =
    Json.writes[AllocatedResourcesUtilization]
  implicit val nodeResourcesWrites: Writes[NodeResources] = Json.writes[NodeResources]
}

case class NodeResources(nodeId: String,
                         nodeName: String,
                         totalResources: TotalResources,
                         hostResources: HostResources,
                         allocatedResources: Map[String, AllocatedResources],
                         allocatedResourcesUtilization: Map[String, AllocatedResourcesUtilization],
                         totalAllocated: TotalResources,
                         totalUtilized: TotalUtilization
                        )

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
  * @param memory Memory in bytes
  */
case class AllocatedResourcesUtilization(id: String, name: String, cpu: Long, memory: Long)

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

/**
  * @param cpu in MHz
  * @param memory in bytes
  */
case class TotalUtilization(cpu: Long, memory: Long)
