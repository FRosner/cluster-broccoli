package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import de.frosner.broccoli.services.{AboutInfoService, InstanceNotFoundException, InstanceService}
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.test._
import Instance.instanceApiWrites
import InstanceCreation.{instanceCreationReads, instanceCreationWrites}
import play.api.http.HeaderNames

import scala.util.{Failure, Success}

class InstanceControllerSpec extends PlaySpecification with AuthUtils {

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
    role = Role.NormalUser
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
            name = "secret",
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
    periodicRuns = Iterable.empty
  )
  val instances = Seq(instanceWithStatus)

  "list" should {

    "list all instances" in new WithApplication {
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.list(None)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
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
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances ++ List(notMatchingInstance)),
          securityService = securityService
        )
      } {
        controller => controller.list(Some(instanceWithStatus.instance.template.id))
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instances))
      }
    }

    "censor secret variables if running in operator mode" in new WithApplication {
      // TODO helper function to test against multiple roles (allowed and not allowed ones)
      testWithAllAuths {
        operator
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.list(Some(instanceWithStatus.instance.template.id))
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "censor secret variables if running in user mode" in new WithApplication {
      // TODO helper function to test against multiple roles (allowed and not allowed ones)
      testWithAllAuths {
        user
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.list(Some(instanceWithStatus.instance.template.id))
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
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
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances ++ List(matchingInstance)),
          securityService = securityService
        )
      } {
        controller => controller.list(None)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(List(matchingInstance)))
      }
    }

  }

  "show" should {

    "return the requested instance if it exists" in new WithApplication {
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.show(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "return 404 if the requested instance does not exist" in new WithApplication {
      val notExisting = "id"
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.getInstance(notExisting)).thenReturn(None)
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.show(notExisting)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 404
      }
    }

    "censor secret variables if running in operator mode" in new WithApplication {
      // TODO helper function (see above)
      testWithAllAuths {
        operator
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.show(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "censor secret variables if running in user mode" in new WithApplication {
      // TODO helper function (see above)
      testWithAllAuths {
        user
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.show(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result).toString must not contain "thisshouldnotappearanywhere")
      }
    }

    "return 404 if the instance does not match the account regex" in new WithApplication {
      testWithAllAuths {
        accountWithRegex
      } {
        securityService => InstanceController(
          instanceService = withInstances(mock(classOf[InstanceService]), instances),
          securityService = securityService
        )
      } {
        controller => controller.show(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 404
      }
    }

  }

  "create" should {

    "create a new instance if it does not exist" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(Json.toJson(instanceCreation))
      } {
        (controller, result) => (status(result) must be equalTo 201) and
          (headers(result).get(HeaderNames.LOCATION) === Some(s"/api/v1/instances/${instanceWithStatus.instance.id}")) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "fail if the creation request is not a valid JSON" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withTextBody("yup")
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the creation request is valid JSON but does not contain all required fields" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(JsObject(Map("x" -> JsString("y"))))
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance cannot be created" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Failure(new IllegalArgumentException("")))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(Json.toJson(instanceCreation))
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance ID does not match the account prefix" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )
      when(instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
      testWithAllAuths {
        accountWithRegex
      } {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(Json.toJson(instanceCreation))
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if running in operator mode" in new WithApplication {
      // TODO helper function (see above)
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )

      testWithAllAuths {
        operator
      } {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(Json.toJson(instanceCreation))
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if running in user mode" in new WithApplication {
      // TODO helper function (see above)
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      val instanceCreation = InstanceCreation(
        templateId = "template",
        parameters = Map(
          "id" -> "id"
        )
      )

      testWithAllAuths {
        user
      } {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.create
      } {
        request => request.withJsonBody(Json.toJson(instanceCreation))
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

  }

  "update" should {

    "update the instance status correctly" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.updateInstance(
        id = instanceWithStatus.instance.id,
        statusUpdater = Some(JobStatus.Running),
        parameterValuesUpdater = None,
        templateSelector = None
      )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "status" -> Json.toJson(JobStatus.Running)
          ))
        )
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "update the instance parameters correctly" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.updateInstance(
        id = instanceWithStatus.instance.id,
        statusUpdater = None,
        parameterValuesUpdater = Some(Map(
          "id" -> "new"
        )),
        templateSelector = None
      )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "parameterValues" -> JsObject(Map(
              "id" -> JsString("new")
            ))
          ))
        )
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "update the instance template correctly" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.updateInstance(
        id = instanceWithStatus.instance.id,
        statusUpdater = None,
        parameterValuesUpdater = None,
        templateSelector = Some("newTemplate")
      )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "selectedTemplate" -> JsString("newTemplate")
          ))
        )
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "fail if the instance does not exist" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.updateInstance(
        id = instanceWithStatus.instance.id,
        statusUpdater = None,
        parameterValuesUpdater = None,
        templateSelector = Some("newTemplate")
      )).thenReturn(Failure(InstanceNotFoundException(instanceWithStatus.instance.id)))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "selectedTemplate" -> JsString("newTemplate")
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance does not match the account instance prefix" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      when(instanceService.updateInstance(
        id = instanceWithStatus.instance.id,
        statusUpdater = Some(JobStatus.Running),
        parameterValuesUpdater = None,
        templateSelector = None
      )).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        accountWithRegex
      } {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "status" -> Json.toJson(JobStatus.Running)
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the request is not JSON" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)
      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withTextBody("bla")
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the request is an empty object" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map.empty[String, JsValue])
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance status request is not valid" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "status" -> JsObject(Map.empty[String, JsValue])
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance parameter request is not valid" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "parameterValues" -> JsObject(Map(
              "id" -> JsObject(Map.empty[String, JsValue])
            ))
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "fail if the instance template request is not valid" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "selectedTemplate" -> JsObject(Map.empty[String, JsValue])
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "not allow instance status updates if not running in admin or operator mode" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      testWithAllAuths(user) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "status" -> Json.toJson(JobStatus.Running)
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "not allow instance parameter updates if not running in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      val operatorMatchers = testWithAllAuths(operator) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "parameterValues" -> JsObject(Map(
              "id" -> JsString("new")
            ))
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
      val userMatchers = testWithAllAuths(user) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "parameterValues" -> JsObject(Map(
              "id" -> JsString("new")
            ))
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
      operatorMatchers and userMatchers
    }

    "not allow template updates if not running in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), List.empty)

      val operatorMatchers = testWithAllAuths(operator) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "selectedTemplate" -> JsString("newTemplate")
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
      val userMatchers = testWithAllAuths(user) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.update(instanceWithStatus.instance.id)
      } {
        request => request.withJsonBody(
          JsObject(Map(
            "selectedTemplate" -> JsString("newTemplate")
          ))
        )
      } {
        (controller, result) => status(result) must be equalTo 400
      }
      operatorMatchers and userMatchers
    }

  }

  "delete" should {

    "delete the instance correctly" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id)).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => (status(result) must be equalTo 200) and
          (contentAsJson(result) must be equalTo Json.toJson(instanceWithStatus))
      }
    }

    "not succeed if the instance does not exist" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id))
        .thenReturn(Failure(InstanceNotFoundException(instanceWithStatus.instance.id)))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "not succeed if the instance cannot be deleted" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id))
        .thenReturn(Failure(new Exception("")))

      testWithAllAuths {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "not succeed if the instance id does not match the account prefix" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), instances)
      when(instanceService.deleteInstance(instanceWithStatus.instance.id)).thenReturn(Success(instanceWithStatus))

      testWithAllAuths {
        accountWithRegex
      } {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 400
      }
    }

    "should only be allowed in administrator mode" in new WithApplication {
      val instanceService = withInstances(mock(classOf[InstanceService]), instances)

      val operatorMatcher = testWithAllAuths(operator) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 400
      }
      val userMatcher = testWithAllAuths(user) {
        securityService => InstanceController(
          instanceService = instanceService,
          securityService = securityService
        )
      } {
        controller => controller.delete(instanceWithStatus.instance.id)
      } {
        request => request
      } {
        (controller, result) => status(result) must be equalTo 403
      }
      operatorMatcher and userMatcher
    }

  }

  "Instance 'anonymization' should" should {

    "remove one secret parameter value" in {
      val originalInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> "noone knows"
          )
        ),
        status = JobStatus.Unknown,
        services = Iterable.empty,
        periodicRuns = Iterable.empty
      )
      val expectedInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> null
          )
        ),
        status = JobStatus.Unknown,
        services = Iterable.empty,
        periodicRuns = Iterable.empty
      )
      InstanceController.removeSecretVariables(originalInstance) === expectedInstance
    }

    "remove multiple secret parameter values" in {
      val originalInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              ),
              "id" -> ParameterInfo(
                name = "secret id",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> "i",
            "password" -> "noone knows"
          )
        ),
        status = JobStatus.Unknown,
        services = Iterable.empty,
        periodicRuns = Iterable.empty
      )
      val expectedInstance = InstanceWithStatus(
        instance = Instance(
          id = "i",
          template = Template(
            id = "t",
            template = "{{id}} {{password}}",
            description = "d",
            parameterInfos = Map(
              "password" -> ParameterInfo(
                name = "secret password",
                default = None,
                secret = Some(true)
              ),
              "id" -> ParameterInfo(
                name = "secret id",
                default = None,
                secret = Some(true)
              )
            )
          ),
          parameterValues = Map(
            "id" -> null,
            "password" -> null
          )
        ),
        status = JobStatus.Unknown,
        services = Iterable.empty,
        periodicRuns = Iterable.empty
      )
      InstanceController.removeSecretVariables(originalInstance) === expectedInstance
    }

  }

}
