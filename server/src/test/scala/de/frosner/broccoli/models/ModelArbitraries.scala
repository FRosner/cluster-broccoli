package de.frosner.broccoli.models

import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.models.JobStatus.JobStatus
import de.frosner.broccoli.models.ServiceStatus.ServiceStatus
import de.frosner.broccoli.nomad.models.{ClientStatus, TaskState}
import org.scalacheck.{Arbitrary, Gen}
import shapeless.tag
import squants.information.Megabytes
import squants.time.Gigahertz

/**
  * Scalacheck arbitrary instances for Broccoli models.
  */
trait ModelArbitraries {

  implicit val arbitraryRole: Arbitrary[Role] = Arbitrary(Gen.oneOf(Role.values))

  implicit def arbitraryAccount(implicit arbRole: Arbitrary[Role]): Arbitrary[Account] = Arbitrary {
    for {
      id <- Gen.identifier.label("id")
      password <- Gen.identifier.label("password")
      instanceRegex <- Gen.identifier.label("instanceRegex")
      role <- arbRole.arbitrary
    } yield Account(id, password, instanceRegex, role)
  }

  implicit var arbitraryParameterInfo: Arbitrary[ParameterInfo] = Arbitrary {
    for {
      id <- Gen.identifier.label("id")
      name <- Gen.option(Gen.identifier.label("name"))
      default <- Gen.option(Gen.identifier).label("default")
      secret <- Gen.option(Gen.oneOf(true, false)).label("secret")
      type_ <- Gen.option(Gen.oneOf(ParameterType.String, ParameterType.Raw)).label("type")
    } yield
      ParameterInfo(
        id = id,
        name = name,
        default = default,
        secret = secret,
        `type` = type_
      )
  }

  implicit def arbitraryTemplate(
      implicit arbParameterInfo: Arbitrary[ParameterInfo]
  ): Arbitrary[Template] = Arbitrary {
    for {
      templateId <- Gen.identifier.label("id")
      templateDescription <- Gen.identifier.label("description")
      templateParameters <- Gen.listOf(arbParameterInfo.arbitrary).label("parameterInfos")
    } yield {
      val idParameter = ParameterInfo(id = "id", None, None, None, None)
      val template = templateParameters.map(i => s"{{${i.id}}}").mkString(" ")
      Template(
        id = templateId,
        description = templateDescription,
        // Templates require an "id" parameter so add one here
        template = s"{{id}} $template",
        parameterInfos = templateParameters.map(i => i.id -> i).toMap + ("id" -> idParameter)
      )
    }
  }

  implicit def arbitraryInstance(implicit arbTemplate: Arbitrary[Template]): Arbitrary[Instance] = Arbitrary {
    for {
      id <- Gen.identifier.label("id")
      template <- arbTemplate.arbitrary.label("template")
      values <- Gen.sequence[Map[String, String], (String, String)](template.parameterInfos.keys.map(id =>
        Gen.identifier.label("value").map(id -> _)))
    } yield Instance(id, template, values)
  }

  implicit val arbitraryJobStatus: Arbitrary[JobStatus] = Arbitrary(Gen.oneOf(JobStatus.values.toSeq))

  implicit val arbitraryServiceStatus: Arbitrary[ServiceStatus] = Arbitrary(Gen.oneOf(ServiceStatus.values.toSeq))

  implicit def arbitraryService(implicit arbStatus: Arbitrary[ServiceStatus]): Arbitrary[Service] = Arbitrary {
    for {
      name <- Gen.identifier.label("name")
      protocol <- Gen.identifier.label("protocol")
      address <- Gen.identifier.label("address")
      port <- Gen.posNum[Int].label("port")
      status <- arbStatus.arbitrary
    } yield Service(name, protocol, address, port, status)
  }

  implicit def arbitraryPeriodicRun(implicit arbJobStatus: Arbitrary[JobStatus]): Arbitrary[PeriodicRun] = Arbitrary {
    for {
      createdBy <- Gen.identifier.label("createdBy")
      jobStatus <- arbitraryJobStatus.arbitrary
      utcSeconds <- Gen.posNum[Long].label("utcSeconds")
      name <- Gen.identifier.label("jobName")
    } yield PeriodicRun(createdBy, jobStatus, utcSeconds, name)
  }

  implicit def arbitraryInstanceWithStatus(
      implicit arbInstance: Arbitrary[Instance],
      arbStatus: Arbitrary[JobStatus],
      arbService: Arbitrary[Service],
      arbRun: Arbitrary[PeriodicRun]
  ): Arbitrary[InstanceWithStatus] =
    Arbitrary {
      for {
        instance <- arbInstance.arbitrary
        jobStatus <- arbStatus.arbitrary
        services <- Gen.listOf(arbService.arbitrary)
        runs <- Gen.listOf(arbRun.arbitrary)
      } yield InstanceWithStatus(instance, jobStatus, services, runs)
    }

  implicit def arbitraryInstanceError(implicit arbRole: Arbitrary[Role]): Arbitrary[InstanceError] =
    Arbitrary(
      Gen.oneOf(
        Gen
          .zip(Gen.identifier.label("instanceId"),
               Gen.option(Gen.identifier.label("message").map(m => new Throwable(m))))
          .map(InstanceError.NotFound.tupled),
        Gen.const(InstanceError.IdMissing),
        Gen.identifier.label("templateId").map(InstanceError.TemplateNotFound),
        Gen.identifier.label("reason").map(InstanceError.InvalidParameters),
        Gen
          .zip(Gen.identifier.label("instanceId"), Gen.identifier.label("regex"))
          .map(InstanceError.UserRegexDenied.tupled),
        Gen.nonEmptyBuildableOf[Set[Role], Role](arbRole.arbitrary).map(InstanceError.RolesRequired(_)),
        Gen.identifier.label("message").map(message => InstanceError.Generic(new Throwable(message)))
      ))

  implicit val arbitraryAllocatedTaskResources: Arbitrary[AllocatedTask.Resources] = Arbitrary(
    for {
      cpuRequired <- Gen
        .option(Gen.chooseNum[Double](0, 3).map(Gigahertz(_)))
        .map(tag[AllocatedTask.CPUUsed](_))
        .label("CPU Used")
      cpuUsed <- Gen
        .option(Gen.chooseNum[Double](0, 3).map(Gigahertz(_)))
        .map(tag[AllocatedTask.CPURequired](_))
        .label("CPU Required")
      memoryRequired <- Gen
        .option(Gen.chooseNum[Int](0, 10000).map(Megabytes(_)))
        .map(tag[AllocatedTask.MemoryRequired](_))
        .label("Memory Used")
      memoryUsed <- Gen
        .option(Gen.chooseNum[Int](0, 10000).map(Megabytes(_)))
        .map(tag[AllocatedTask.MemoryUsed](_))
        .label("Memory Required")
    } yield AllocatedTask.Resources(cpuUsed, cpuRequired, memoryRequired, memoryUsed)
  )

  implicit def arbitraryAllocatedTask(
      implicit arbClientStatus: Arbitrary[ClientStatus],
      arbTaskState: Arbitrary[TaskState],
      arbResources: Arbitrary[AllocatedTask.Resources]
  ): Arbitrary[AllocatedTask] =
    Arbitrary {
      for {
        taskName <- Gen.identifier.label("taskName")
        taskState <- arbTaskState.arbitrary
        allocationId <- Gen.uuid.label("allocationId")
        clientStatus <- arbClientStatus.arbitrary
        resources <- arbResources.arbitrary
      } yield AllocatedTask(taskName, taskState, allocationId.toString, clientStatus, resources)
    }

  implicit def arbitraryInstanceTasks(implicit arbTask: Arbitrary[AllocatedTask]): Arbitrary[InstanceTasks] =
    Arbitrary {
      for {
        id <- Gen.identifier.label("id")
        tasks <- Gen.listOf(arbTask.arbitrary)
      } yield InstanceTasks(id, tasks)
    }
}

/**
  * Import object for arbitrary instances of models.
  */
object arbitraries extends ModelArbitraries
