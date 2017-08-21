package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import de.frosner.broccoli.RemoveSecrets.ToRemoveSecretsOps
import de.frosner.broccoli.instances.NomadInstances
import de.frosner.broccoli.nomad
import de.frosner.broccoli.websocket.{BroccoliMessageHandler, IncomingMessage, OutgoingMessage}
import jp.t2v.lab.play2.auth.test.Helpers._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.test._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.Success

class WebSocketControllerSpec
    extends PlaySpecification
    with AuthUtils
    with ModelArbitraries
    with nomad.ModelArbitraries
    with Mockito
    with ToRemoveSecretsOps {

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
            name = None,
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

  private def wrap(messageType: IncomingMessage.Type, payload: JsValue): JsValue =
    JsObject(
      Map(
        "messageType" -> Json.toJson(messageType),
        "payload" -> payload
      ))

  private def testWs(controllerSetup: SecurityService => WebSocketController,
                     inMsg: IncomingMessage,
                     expectations: Map[Option[(String, Role)], OutgoingMessage]) =
    expectations.foreach {
      case (maybeInstanceRegexAndRole, outMsg) =>
        val maybeAccount = maybeInstanceRegexAndRole.map {
          case (instanceRegex, role) => UserAccount("user", "pass", instanceRegex, role)
        }
        val securityService = maybeAccount
          .map { account =>
            withAuthConf(mock[SecurityService], List(account))
          }
          .getOrElse {
            withAuthNone(mock[SecurityService])
          }
        val controller = controllerSetup(securityService)
        when(controller.webSocketService.newConnection(Matchers.anyString(), any[Account]))
          .thenReturn(Enumerator.empty[Msg])
        when(controller.webSocketService.newConnection(any[Account]))
          .thenReturn(("session_id", Enumerator.empty[Msg]))
        val result = maybeAccount
          .map { account =>
            controller.requestToSocket(FakeRequest().withLoggedIn(controller)(account.name))
          }
          .getOrElse {
            controller.requestToSocket(FakeRequest())
          }
        WsTestUtil.wrapConnection(result) match {
          case Right((incoming, outgoing)) =>
            incoming.feed(Json.toJson(inMsg)).end
            verify(controller.webSocketService)
              .send(Matchers.anyString(), Matchers.eq(Json.toJson(outMsg)))
          case Left(_) => throw new IllegalStateException()
        }
    }

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "WebSocketController" should {

    "establish a websocket connection correctly (with authentication)" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      val controller = WebSocketController(
        webSocketService = mock[WebSocketService],
        templateService = withTemplates(mock[TemplateService], Seq.empty),
        instanceService = instanceService,
        aboutService = withDummyValues(mock[AboutInfoService]),
        securityService = withAuthConf(mock[SecurityService], List(account)),
        messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
        playEnv = playEnv,
        cacheApi = cacheApi
      )
      val result = controller.requestToSocket(FakeRequest().withLoggedIn(controller)(account.name))
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight
    }

    "establish a websocket connection correctly (without authentication)" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      val controller = WebSocketController(
        webSocketService = mock[WebSocketService],
        templateService = withTemplates(mock[TemplateService], Seq.empty),
        instanceService = instanceService,
        aboutService = withDummyValues(mock[AboutInfoService]),
        securityService = withAuthNone(mock[SecurityService]),
        messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
        playEnv = playEnv,
        cacheApi = cacheApi
      )
      when(controller.webSocketService.newConnection(any[Account])).thenReturn(("id", null))
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight
    }

    "decline the websocket connection if not authenticated" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      val controller = WebSocketController(
        webSocketService = mock[WebSocketService],
        templateService = withTemplates(mock[TemplateService], Seq.empty),
        instanceService = instanceService,
        aboutService = withDummyValues(mock[AboutInfoService]),
        securityService = withAuthConf(mock[SecurityService], List(account)),
        messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
        playEnv = playEnv,
        cacheApi = cacheApi
      )
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beLeft.like {
        case d => d.header.status === 403
      }
    }

    "send about info, template and instance list after establishing the connection" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val instances = Seq(
        instanceWithStatus
      )
      val templates = Seq.empty[Template]
      private val instanceService = withInstances(mock[InstanceService], instances)
      val controller = WebSocketController(
        webSocketService = mock[WebSocketService],
        templateService = withTemplates(mock[TemplateService], templates),
        instanceService = instanceService,
        aboutService = withDummyValues(mock[AboutInfoService]),
        securityService = withAuthNone(mock[SecurityService]),
        messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
        playEnv = playEnv,
        cacheApi = cacheApi
      )
      when(controller.webSocketService.newConnection(any[Account])).thenReturn(("id", Enumerator.empty[Msg]))
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight.like {
        case (incoming, outgoing) =>
          val messages = outgoing.get
          (messages should haveSize(3)) and
            (messages should contain(
              Json.toJson(OutgoingMessage.ListTemplates(templates)),
              Json.toJson(OutgoingMessage.ListInstances(instances)),
              Json.toJson(OutgoingMessage.AboutInfoMsg(controller.aboutService.aboutInfo(null)))
            ))
      }
    }

    "process instance addition requests if no auth is enabled" in new WithApplication {
      val id = "id"
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      val controller = WebSocketController(
        webSocketService = mock[WebSocketService],
        templateService = withTemplates(mock[TemplateService], Seq.empty),
        instanceService = instanceService,
        aboutService = withDummyValues(mock[AboutInfoService]),
        securityService = withAuthNone(mock[SecurityService]),
        messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
        playEnv = playEnv,
        cacheApi = cacheApi
      )
      when(controller.webSocketService.newConnection(any[Account])).thenReturn((id, Enumerator.empty[Msg]))
      val instanceCreation = InstanceCreation(
        "template",
        Map(
          "id" -> "blib"
        )
      )
      when(controller.instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection match {
        case Right((incoming, outgoing)) =>
          val resultMsg = OutgoingMessage.AddInstanceSuccess(
            InstanceCreated(
              instanceCreation,
              instanceWithStatus
            )
          )
          incoming.feed(Json.toJson(IncomingMessage.AddInstance(instanceCreation))).end
          verify(controller.webSocketService).send(id, Json.toJson(resultMsg))
        case Left(_) => throw new IllegalStateException()
      }
    }

    "process instance addition correctly" in new WithApplication {
      val instanceCreation = InstanceCreation(
        "template",
        Map(
          "id" -> "blib"
        )
      )

      val success = OutgoingMessage.AddInstanceSuccess(
        InstanceCreated(
          instanceCreation,
          instanceWithStatus
        )
      )
      val roleFailure = OutgoingMessage.AddInstanceError(InstanceError.RolesRequired(Role.Administrator))
      val regexFailure =
        OutgoingMessage.AddInstanceError(InstanceError.UserRegexDenied("blib", "bla"))

      testWs(
        controllerSetup = { securityService =>
          val instanceService = withInstances(mock[InstanceService], Seq.empty)
          val controller = WebSocketController(
            webSocketService = mock[WebSocketService],
            templateService = withTemplates(mock[TemplateService], Seq.empty),
            instanceService = instanceService,
            aboutService = withDummyValues(mock[AboutInfoService]),
            securityService = securityService,
            messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
            playEnv = playEnv,
            cacheApi = cacheApi
          )
          when(controller.instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingMessage.AddInstance(instanceCreation),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> regexFailure,
          Some((".*", Role.Operator)) -> roleFailure,
          Some((".*", Role.User)) -> roleFailure
        )
      )
    }

    "process instance deletion correctly" in new WithApplication {
      val instanceDeletion = "id"

      val success = OutgoingMessage.DeleteInstanceSuccess(
        InstanceDeleted(
          instanceDeletion,
          instanceWithStatus
        )
      )
      val roleFailure = OutgoingMessage.DeleteInstanceError(InstanceError.RolesRequired(Role.Administrator))
      val regexFailure = OutgoingMessage.DeleteInstanceError(InstanceError.UserRegexDenied(instanceDeletion, "bla"))

      testWs(
        controllerSetup = { securityService =>
          val instanceService = withInstances(mock[InstanceService], Seq.empty)
          val controller = WebSocketController(
            webSocketService = mock[WebSocketService],
            templateService = withTemplates(mock[TemplateService], Seq.empty),
            instanceService = instanceService,
            aboutService = withDummyValues(mock[AboutInfoService]),
            securityService = securityService,
            messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
            playEnv = playEnv,
            cacheApi = cacheApi
          )
          when(controller.instanceService.deleteInstance(instanceDeletion)).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingMessage.DeleteInstance(instanceDeletion),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> regexFailure,
          Some((".*", Role.Operator)) -> roleFailure,
          Some((".*", Role.User)) -> roleFailure
        )
      )
    }

    "process instance parameter updates correctly" in new WithApplication {
      val instanceUpdate = InstanceUpdate(
        instanceId = Some("id"),
        status = None,
        parameterValues = Some(
          Map(
            "id" -> "blib"
          )
        ),
        selectedTemplate = None
      )

      val success = OutgoingMessage.UpdateInstanceSuccess(
        InstanceUpdated(
          instanceUpdate,
          instanceWithStatus
        )
      )
      testWs(
        controllerSetup = { securityService =>
          val instanceService = withInstances(mock[InstanceService], Seq.empty)
          val controller = WebSocketController(
            webSocketService = mock[WebSocketService],
            templateService = withTemplates(mock[TemplateService], Seq.empty),
            instanceService = instanceService,
            aboutService = withDummyValues(mock[AboutInfoService]),
            securityService = securityService,
            messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
            playEnv = playEnv,
            cacheApi = cacheApi
          )
          when(
            controller.instanceService.updateInstance(
              id = instanceUpdate.instanceId.get,
              statusUpdater = instanceUpdate.status,
              parameterValuesUpdater = instanceUpdate.parameterValues,
              templateSelector = instanceUpdate.selectedTemplate
            )).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingMessage.UpdateInstance(instanceUpdate),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.UserRegexDenied(instanceUpdate.instanceId.get, "bla")),
          Some((".*", Role.Operator)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.RolesRequired(Role.Administrator)
          ),
          Some((".*", Role.User)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.RolesRequired(Role.Administrator, Role.Operator)
          )
        )
      )
    }

    "process instance status updates correctly" in new WithApplication {
      val instanceUpdate = InstanceUpdate(
        instanceId = Some("id"),
        status = Some(JobStatus.Running),
        parameterValues = None,
        selectedTemplate = None
      )

      val success = OutgoingMessage.UpdateInstanceSuccess(
        InstanceUpdated(
          instanceUpdate,
          instanceWithStatus
        )
      )
      val secretSuccess = OutgoingMessage.UpdateInstanceSuccess(
        InstanceUpdated(
          instanceUpdate,
          instanceWithStatus.removeSecrets
        )
      )
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock[WebSocketService],
            templateService = withTemplates(mock[TemplateService], Seq.empty),
            instanceService = instanceService,
            aboutService = withDummyValues(mock[AboutInfoService]),
            securityService = securityService,
            messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
            playEnv = playEnv,
            cacheApi = cacheApi
          )
          when(
            controller.instanceService.updateInstance(
              id = instanceUpdate.instanceId.get,
              statusUpdater = instanceUpdate.status,
              parameterValuesUpdater = instanceUpdate.parameterValues,
              templateSelector = instanceUpdate.selectedTemplate
            )).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingMessage.UpdateInstance(instanceUpdate),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.UserRegexDenied(instanceUpdate.instanceId.get, "bla")
          ),
          Some((".*", Role.Operator)) -> secretSuccess,
          Some((".*", Role.User)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.RolesRequired(Role.Administrator, Role.Operator)
          )
        )
      )
    }

    "process instance template updates correctly" in new WithApplication {
      val instanceUpdate = InstanceUpdate(
        instanceId = Some("id"),
        status = None,
        parameterValues = None,
        selectedTemplate = Some("templateId")
      )

      val success = OutgoingMessage.UpdateInstanceSuccess(
        InstanceUpdated(
          instanceUpdate,
          instanceWithStatus
        )
      )
      val instanceService = withInstances(mock[InstanceService], Seq.empty)
      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock[WebSocketService],
            templateService = withTemplates(mock[TemplateService], Seq.empty),
            instanceService = instanceService,
            aboutService = withDummyValues(mock[AboutInfoService]),
            securityService = securityService,
            messageHandler = new BroccoliMessageHandler(mock[NomadInstances], instanceService),
            playEnv = playEnv,
            cacheApi = cacheApi
          )
          when(
            controller.instanceService.updateInstance(
              id = instanceUpdate.instanceId.get,
              statusUpdater = instanceUpdate.status,
              parameterValuesUpdater = instanceUpdate.parameterValues,
              templateSelector = instanceUpdate.selectedTemplate
            )).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingMessage.UpdateInstance(instanceUpdate),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.UserRegexDenied(instanceUpdate.instanceId.get, "bla")
          ),
          Some((".*", Role.Operator)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.RolesRequired(Role.Administrator)
          ),
          Some((".*", Role.User)) -> OutgoingMessage.UpdateInstanceError(
            InstanceError.RolesRequired(Role.Administrator, Role.Operator)
          )
        )
      )
    }

  }

}
