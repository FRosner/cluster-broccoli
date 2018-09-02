package de.frosner.broccoli.nomad.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

object NodeResources {
  implicit val memoryInfoReads: Reads[MemoryInfo] = (
    (JsPath \ "Available").read[Long] and
      (JsPath \ "Free").read[Long] and
      (JsPath \ "Total").read[Long] and
      (JsPath \ "Used").read[Long]
    )(MemoryInfo.apply _)

  implicit val memoryInfoWrites: Writes[MemoryInfo] = (
    (JsPath \ "Available").write[Long] and
      (JsPath \ "Free").write[Long] and
      (JsPath \ "Total").write[Long] and
      (JsPath \ "Used").write[Long]
  )(memoryInfo => (memoryInfo.available, memoryInfo.free, memoryInfo.total, memoryInfo.used))

  implicit val diskInfoReads: Reads[DiskInfo] = (
    (JsPath \ "Available").read[Long] and
      (JsPath \ "Device").read[String] and
      (JsPath \ "InodesUsedPercent").read[Double] and
      (JsPath \ "Mountpoint").read[String] and
      (JsPath \ "Size").read[Long] and
      (JsPath \ "Used").read[Long] and
      (JsPath \ "UsedPercent").read[Double]
    )(DiskInfo.apply _)

  implicit val diskInfoWrites: Writes[DiskInfo] = (
    (JsPath \ "Available").write[Long] and
      (JsPath \ "Device").write[String] and
      (JsPath \ "InodesUsedPercent").write[Double] and
      (JsPath \ "Mountpoint").write[String] and
      (JsPath \ "Size").write[Long] and
      (JsPath \ "Used").write[Long] and
      (JsPath \ "UsedPercent").write[Double]
    )(diskInfo =>
    (diskInfo.available, diskInfo.device, diskInfo.inodesUsedPercent, diskInfo.mountpoint, diskInfo.size, diskInfo.used,
      diskInfo.usedPercent))

  implicit val cpuInfoReads: Reads[CPUInfo] = (
    (JsPath \ "CPU").read[String] and
      (JsPath \ "Idle").read[Double] and
      (JsPath \ "System").read[Double] and
      (JsPath \ "Total").read[Double] and
      (JsPath \ "User").read[Double]
    )(CPUInfo.apply _)

  implicit val cpuInfoWrites: Writes[CPUInfo] = (
    (JsPath \ "CPU").write[String] and
      (JsPath \ "Idle").write[Double] and
      (JsPath \ "System").write[Double] and
      (JsPath \ "Total").write[Double] and
      (JsPath \ "User").write[Double]
    )(cpuInfo => (cpuInfo.cpuName, cpuInfo.idle, cpuInfo.system, cpuInfo.total, cpuInfo.user))

  implicit val allocDirInfoReads: Reads[AllocDirInfo] = (
    (JsPath \ "Available").read[Long] and
      (JsPath \ "Device").read[String] and
      (JsPath \ "InodesUsedPercent").read[Double] and
      (JsPath \ "Mountpoint").read[String] and
      (JsPath \ "Size").read[Long] and
      (JsPath \ "Used").read[Long] and
      (JsPath \ "UsedPercent").read[Double]
    )(AllocDirInfo.apply _)

  implicit val allocDirInfoWrites: Writes[AllocDirInfo] = (
    (JsPath \ "Available").write[Long] and
      (JsPath \ "Device").write[String] and
      (JsPath \ "InodesUsedPercent").write[Double] and
      (JsPath \ "Mountpoint").write[String] and
      (JsPath \ "Size").write[Long] and
      (JsPath \ "Used").write[Long] and
      (JsPath \ "UsedPercent").write[Double]
    )(allocDirInfo =>
    (allocDirInfo.available, allocDirInfo.device, allocDirInfo.inodesUsedPercent, allocDirInfo.mountPoint,
      allocDirInfo.size, allocDirInfo.used, allocDirInfo.usedPercent))

  implicit val resourceInfoReads: Reads[ResourceInfo] = (
    (JsPath \ "AllocDirStats").read[AllocDirInfo] and
      (JsPath \ "CPU").read[Seq[CPUInfo]] and
      (JsPath \ "CPUTicksConsumed").read[Double] and
      (JsPath \ "DiskStats").read[Seq[DiskInfo]] and
      (JsPath \ "Memory").read[MemoryInfo] and
      (JsPath \ "Timestamp").read[Long] and
      (JsPath \ "Uptime").read[Long]
    )(ResourceInfo.apply _)

  implicit val resourceInfoWrites: Writes[ResourceInfo] = (
    (JsPath \ "AllocDirStats").write[AllocDirInfo] and
      (JsPath \ "CPU").write[Seq[CPUInfo]] and
      (JsPath \ "CPUTicksConsumed").write[Double] and
      (JsPath \ "DiskStats").write[Seq[DiskInfo]] and
      (JsPath \ "Memory").write[MemoryInfo] and
      (JsPath \ "Timestamp").write[Long] and
      (JsPath \ "Uptime").write[Long]
    )(resourceInfo =>
    (resourceInfo.allocDirStats, resourceInfo.cpusStats, resourceInfo.cpuTicksConsumed, resourceInfo.disksStats,
      resourceInfo.memoryStats, resourceInfo.timestamp, resourceInfo.uptime))

  implicit val nodeResourcesWrites: Writes[NodeResources] = Json.writes[NodeResources]
}

case class NodeResources(nodeId: String, nodeName: String, resources: ResourceInfo)

case class ResourceInfo(allocDirStats: AllocDirInfo, cpusStats: Seq[CPUInfo], cpuTicksConsumed: Double,
                        disksStats: Seq[DiskInfo], memoryStats: MemoryInfo, timestamp: Long, uptime: Long)

case class AllocDirInfo(available: Long, device: String, inodesUsedPercent: Double, mountPoint: String,
                        size: Long, used: Long, usedPercent: Double)

case class CPUInfo(cpuName: String, idle: Double, system: Double, total: Double, user: Double)

case class DiskInfo(available: Long, device: String, inodesUsedPercent: Double, mountpoint: String, size: Long,
                    used: Long, usedPercent: Double)

case class MemoryInfo(available: Long, free: Long, total: Long, used: Long)
