package de.frosner.broccoli.controllers

import cats.data.EitherT
import cats.instances.future._
import de.frosner.broccoli.http.ToHTTPResult
import de.frosner.broccoli.instances.NomadInstances
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad
import de.frosner.broccoli.services.{InstanceNotFoundException, InstanceService, SecurityService}
import jp.t2v.lab.play2.auth.test.{Helpers => AuthHelpers}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import play.api.Environment
import play.api.cache.CacheApi
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.test._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class InstanceControllerSpec
    extends PlaySpecification
    with Mockito
    with AuthUtils
    with ScalaCheck
    with nomad.ModelArbitraries
    with ModelArbitraries
    with ToHTTPResult.ToToHTTPResultOps
    with AuthHelpers {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  val accountWithRegex = UserAccount(
    name = "user",
    password = "pass",
    instanceRegex = "^matching-.*",
    role = Role.Administrator
  )

  val operator = UserAccount(
    name = "Operator",
    password = "pass",
    instanceRegex = ".*",
    role = Role.Operator
  )

  val user = UserAccount(
    name = "User",
    password = "pass",
    instanceRegex = ".*",
    role = Role.User
  )

  val instanceWithStatus = InstanceWithStatus(
    instance = Instance(
      id = "i",
      template = Template(
        id = "t",
        template = "{{id}} {{secret}}",
        description = "d",
        parameterInfos = Map(
          "secret" -> ParameterInfo(
            id = "secret",
            name = Some("secret"),
            default = Some("value"),
            secret = Some(true)
          )
        )
      ),
      parameterValues = Map(
        "id" -> "i",
        "secret" -> "thisshouldnotappearanywhere"
      )
    ),
    status = JobStatus.Unknown,
    services = List(
      Service(
        name = "n",
        protocol = "http",
        address = "localhost",
        port = 8888,
        status = ServiceStatus.Unknown
      )
    ),
    periodicRuns = Seq.empty
  )
  val instances = Seq(instanceWithStatus)

  "list" should {

    "list all instances" in new WithApplication {
      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.list(None)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instances))
      }
    }

    "list only instance of the specified template" in new WithApplication {
      val notMatchingInstance = instanceWithStatus.copy(
        instance = instanceWithStatus.instance.copy(
          template = instanceWithStatus.instance.template.copy(
            id = "notMatching"
          )
        )
      )
      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances ++ List(notMatchingInstance)),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.list(Some(instanceWithStatus.instance.template.id))
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instances))
      }
    }

    "censor secret variables if running in operator mode" in new WithApplication {
      // TODO helper function to test against multiple roles (allowed and not allowed ones)
      testWithAllAuths {
        operator
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.list(Some(instanceWithStatus.instance.template.id))
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "censor secret variables if running in user mode" in new WithApplication {
      // TODO helper function to test against multiple roles (allowed and not allowed ones)
      testWithAllAuths {
        user
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.list(Some(instanceWithStatus.instance.template.id))
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "filter based on the instanceRegex defined in the account settings" in new WithApplication {
      val matchingInstance = instanceWithStatus.copy(
        instance = instanceWithStatus.instance.copy(
          id = "matching-"
        )
      )
      testWithAllAuths {
        accountWithRegex
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances ++ List(matchingInstance)),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.list(None)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(List(matchingInstance)))
      }
    }

  }

  "show" should {

    "return the requested instance if it exists" in new WithApplication {
      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.show(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "return 404 if the requested instance does not exist" in new WithApplication {
      val notExisting = "id"
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(instanceService.getInstance(notExisting)).thenReturn(None)
      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.show(notExisting)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 404
      }
    }

    "censor secret variables if running in operator mode" in new WithApplication {
      // TODO helper function (see above)
      testWithAllAuths {
        operator
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.show(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "censor secret variables if running in user mode" in new WithApplication {
      // TODO helper function (see above)
      testWithAllAuths {
        user
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.show(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "return 403 if the instance does not match the account regex" in new WithApplication {
      testWithAllAuths {
        accountWithRegex
      } { securityService =>
        InstanceController(
          instanceService = withInstances(mock[InstanceService], instances),
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.show(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

  }

  "tasks" should {
    "return tasks from the instance service" in { implicit ee: ExecutionEnv =>
      prop { (user: Account, instanceTasks: InstanceTasks) =>
        val securityService = mock[SecurityService]
        securityService.authMode returns "conf"
        securityService.isAllowedToAuthenticate(Matchers.any[Credentials]) returns true
        securityService.getAccount(user.name) returns Some(user)

        val instances = mock[NomadInstances]
        instances.getInstanceTasks(user)(instanceTasks.instanceId) returns EitherT.pure[Future, InstanceError](
          instanceTasks)

        val controller = InstanceController(
          instances,
          mock[InstanceService],
          securityService,
          mock[CacheApi],
          Environment.simple()
        )

        val request = FakeRequest().withBody(()).withLoggedIn(controller)(user.name)
        val result = controller.tasks(instanceTasks.instanceId)(request)
        status(result) must beEqualTo(instanceTasks.toHTTPResult.header.status)
        contentAsJson(result) must beEqualTo(contentAsJson(Future.successful(instanceTasks.toHTTPResult)))
      }.setContext(new WithApplication() {}).set(minTestsOk = 1)
    }

    "return errors from the instance service" in { implicit ee: ExecutionEnv =>
      prop { (instanceId: String, user: Account, error: InstanceError) =>
        val securityService = mock[SecurityService]
        securityService.authMode returns "conf"
        securityService.isAllowedToAuthenticate(Matchers.any[Credentials]) returns true
        securityService.getAccount(user.name) returns Some(user)
        val instances = mock[NomadInstances]
        instances.getInstanceTasks(user)(instanceId) returns EitherT.leftT[Future, InstanceTasks](error)

        val controller = InstanceController(
          instances,
          mock[InstanceService],
          securityService,
          mock[CacheApi],
          Environment.simple()
        )
        val request = FakeRequest().withBody(()).withLoggedIn(controller)(user.name)

        val result = controller.tasks(instanceId)(request)
        status(result) must beEqualTo(error.toHTTPResult.header.status)
        contentAsJson(result) must beEqualTo(contentAsJson(Future.successful(error.toHTTPResult)))
      }.setGen1(Gen.identifier.label("instanceId")).setContext(new WithApplication() {}).set(minTestsOk = 1)
    }

    "fail if not authenticated" in {
      prop { (instanceId: String) =>
        val securityService = mock[SecurityService]
        securityService.authMode returns "conf"
        val controller = InstanceController(
          mock[NomadInstances],
          mock[InstanceService],
          securityService,
          mock[CacheApi],
          Environment.simple()
        )
        val result = controller.tasks(instanceId)(FakeRequest().withBody(()))
        status(result) must beEqualTo(FORBIDDEN)
      }.setGen(Gen.identifier.label("instanceId")).setContext(new WithApplication() {}).set(minTestsOk = 1)
    }
  }

  "create" should {

    "create a new instance if it does not exist" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.create
      } { request =>
        request.withBody(instanceCreation)
      } { (controller, result) =>
        (status(result) must be equalTo 201) and
          (headers(result).get(HeaderNames.LOCATION) === Some(s"/api/v1/instances/${instanceWithStatus.instance.id}")) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "fail if the instance cannot be created" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Failure(new IllegalArgumentException("")))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.create
      } { request =>
        request.withBody(instanceCreation)
      } { (controller, result) =>
        status(result) must be equalTo 400
      }
    }

    "fail if the instance ID does not match the account prefix" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
      testWithAllAuths {
        accountWithRegex
      } { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.create
      } { request =>
        request.withBody(instanceCreation)
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

    "fail if running in operator mode" in new WithApplication {
      // TODO helper function (see above)
      val instanceService = withInstances(mock[InstanceService], List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )

      testWithAllAuths {
        operator
      } { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.create
      } { request =>
        request.withBody(instanceCreation)
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

    "fail if running in user mode" in new WithApplication {
      // TODO helper function (see above)
      val instanceService = withInstances(mock[InstanceService], List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )

      testWithAllAuths {
        user
      } { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.create
      } { request =>
        request.withBody(instanceCreation)
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

  }

  "update" should {

    "update the instance status correctly" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(
        instanceService.updateInstance(
          id = instanceWithStatus.instance.id,
          statusUpdater = Some(JobStatus.Running),
          parameterValuesUpdater = None,
          templateSelector = None
        )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = Some(JobStatus.Running),
            parameterValues = None,
            selectedTemplate = None
          ))
      } { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "update the instance parameters correctly" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(
        instanceService.updateInstance(
          id = instanceWithStatus.instance.id,
          statusUpdater = None,
          parameterValuesUpdater = Some(
            Map(
              "id" -> "new"
            )),
          templateSelector = None
        )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = Some(Map("id" -> "new")),
            selectedTemplate = None
          ))
      } { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "update the instance template correctly" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(
        instanceService.updateInstance(
          id = instanceWithStatus.instance.id,
          statusUpdater = None,
          parameterValuesUpdater = None,
          templateSelector = Some("newTemplate")
        )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = None,
            selectedTemplate = Some("newTemplate")
          ))
      } { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "fail if the instance does not exist" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(
        instanceService.updateInstance(
          id = instanceWithStatus.instance.id,
          statusUpdater = None,
          parameterValuesUpdater = None,
          templateSelector = Some("newTemplate")
        )).thenReturn(Failure(InstanceNotFoundException(instanceWithStatus.instance.id)))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = None,
            selectedTemplate = Some("newTemplate")
          ))
      } { (controller, result) =>
        status(result) must be equalTo 404
      }
    }

    "fail if the instance does not match the account instance prefix" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)
      when(
        instanceService.updateInstance(
          id = instanceWithStatus.instance.id,
          statusUpdater = Some(JobStatus.Running),
          parameterValuesUpdater = None,
          templateSelector = None
        )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        accountWithRegex
      } { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = Some(JobStatus.Running),
            parameterValues = None,
            selectedTemplate = None
          ))
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

    "fail if the request is an empty object" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = None,
            selectedTemplate = None
          ))
      } { (controller, result) =>
        status(result) must be equalTo 400
      }
    }

    "not allow instance status updates if not running in admin or operator mode" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)

      testWithAllAuths(user) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = Some(JobStatus.Running),
            parameterValues = None,
            selectedTemplate = None
          ))
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

    "not allow instance parameter updates if not running in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)

      val operatorMatchers = testWithAllAuths(operator) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = Some(Map("id" -> "new")),
            selectedTemplate = None
          ))

      } { (controller, result) =>
        status(result) must be equalTo 403
      }
      val userMatchers = testWithAllAuths(user) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = Some(Map("id" -> "new")),
            selectedTemplate = None
          ))

      } { (controller, result) =>
        status(result) must be equalTo 403
      }
      operatorMatchers and userMatchers
    }

    "not allow template updates if not running in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], List.empty)

      val operatorMatchers = testWithAllAuths(operator) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = None,
            selectedTemplate = Some("newTemplate")
          ))
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
      val userMatchers = testWithAllAuths(user) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.update(instanceWithStatus.instance.id)
      } { request =>
        request.withBody(
          InstanceUpdate(
            instanceId = None,
            status = None,
            parameterValues = None,
            selectedTemplate = Some("newTemplate")
          ))
      } { (controller, result) =>
        status(result) must be equalTo 403
      }
      operatorMatchers and userMatchers
    }

  }

  "delete" should {

    "delete the instance correctly" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id)).thenReturn(Success(instanceWithStatus))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          cacheApi = cacheApi,
          playEnv = playEnv,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "not succeed if the instance does not exist" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id))
        .thenReturn(Failure(InstanceNotFoundException(instanceWithStatus.instance.id)))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 400
      }
    }

    "not succeed if the instance cannot be deleted" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id))
        .thenReturn(Failure(new Exception("")))

      testWithAllAuths { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 400
      }
    }

    "not succeed if the instance id does not match the account prefix" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id)).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        accountWithRegex
      } { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 403
      }
    }

    "should only be allowed in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock[InstanceService], instances)

      val operatorMatcher = testWithAllAuths(operator) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 403
      }
      val userMatcher = testWithAllAuths(user) { securityService =>
        InstanceController(
          instanceService = instanceService,
          securityService = securityService,
          playEnv = playEnv,
          cacheApi = cacheApi,
          instances = mock[NomadInstances]
        )
      } { controller =>
        controller.delete(instanceWithStatus.instance.id)
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 403
      }
      operatorMatcher and userMatcher
    }
  }
}
