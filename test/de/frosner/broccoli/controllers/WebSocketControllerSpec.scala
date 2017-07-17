package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import de.frosner.broccoli.services.WebSocketService.Msg
import de.frosner.broccoli.services._
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.test._
import jp.t2v.lab.play2.auth.test.Helpers._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import ParameterInfo.{parameterInfoReads, parameterInfoWrites}
import de.frosner.broccoli.controllers.IncomingWsMessageType.IncomingWsMessageType
import de.frosner.broccoli.controllers.OutgoingWsMessageType.OutgoingWsMessageType
import de.frosner.broccoli.models.Role.Role
import org.mockito.Matchers

import scala.util.{Failure, Success}

class WebSocketControllerSpec extends PlaySpecification with AuthUtils {

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
    periodicRuns = Iterable.empty
  )

  private def wrap(messageType: IncomingWsMessageType, payload: JsValue): JsValue =
    JsObject(
      Map(
        "messageType" -> Json.toJson(messageType),
        "payload" -> payload
      ))

  private implicit val incomingWsMessagesWrites: Writes[IncomingWsMessage] = Writes {
    case IncomingWsMessage(IncomingWsMessageType.AddInstance, payload: InstanceCreation) =>
      wrap(IncomingWsMessageType.AddInstance, Json.toJson(payload))
    case IncomingWsMessage(IncomingWsMessageType.DeleteInstance, payload: String) =>
      wrap(IncomingWsMessageType.DeleteInstance, Json.toJson(payload))
    case IncomingWsMessage(IncomingWsMessageType.UpdateInstance, payload: InstanceUpdate) =>
      wrap(IncomingWsMessageType.UpdateInstance, Json.toJson(payload))
  }

  private def testWs(controllerSetup: SecurityService => WebSocketController,
                     inMsg: IncomingWsMessage,
                     expectations: Map[Option[(String, Role)], OutgoingWsMessage]) =
    expectations.foreach {
      case (maybeInstanceRegexAndRole, outMsg) =>
        val maybeAccount = maybeInstanceRegexAndRole.map {
          case (instanceRegex, role) => UserAccount("user", "pass", instanceRegex, role)
        }
        val securityService = maybeAccount
          .map { account =>
            withAuthConf(mock(classOf[SecurityService]), List(account))
          }
          .getOrElse {
            withAuthNone(mock(classOf[SecurityService]))
          }
        val controller = controllerSetup(securityService)
        when(controller.webSocketService.newConnection(anyString(), any(classOf[Account])))
          .thenReturn(Enumerator.empty[Msg])
        when(controller.webSocketService.newConnection(any(classOf[Account])))
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
            incoming.feed(Json.toJson(inMsg)(incomingWsMessagesWrites)).end
            verify(controller.webSocketService)
              .send(anyString(), Matchers.eq(Json.toJson(outMsg)(OutgoingWsMessage.outgoingWsMessageWrites)))
        }
    }

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "WebSocketController" should {

    "establish a websocket connection correctly (with authentication)" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
      )
      val result = controller.requestToSocket(FakeRequest().withLoggedIn(controller)(account.name))
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight
    }

    "establish a websocket connection correctly (without authentication)" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )
      when(controller.webSocketService.newConnection(any(classOf[Account]))).thenReturn(("id", null))
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight
    }

    "decline the websocket connection if not authenticated" in new WithApplication {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
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
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), templates),
        instanceService = withInstances(mock(classOf[InstanceService]), instances),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )
      when(controller.webSocketService.newConnection(any(classOf[Account]))).thenReturn(("id", Enumerator.empty[Msg]))
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = WsTestUtil.wrapConnection(result)
      maybeConnection should beRight.like {
        case (incoming, outgoing) =>
          val messages = outgoing.get
          (messages should haveSize(3)) and
            (messages should contain(
              Json.toJson(OutgoingWsMessage(OutgoingWsMessageType.ListTemplatesMsg, templates)),
              Json.toJson(OutgoingWsMessage(OutgoingWsMessageType.ListInstancesMsg, instances)),
              Json.toJson(
                OutgoingWsMessage(OutgoingWsMessageType.AboutInfoMsg, controller.aboutService.aboutInfo(null)))
            ))
      }
    }

    "process instance addition requests if no auth is enabled" in new WithApplication {
      val id = "id"
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthNone(mock(classOf[SecurityService]))
      )
      when(controller.webSocketService.newConnection(any(classOf[Account]))).thenReturn((id, Enumerator.empty[Msg]))
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
          val creationMsg = IncomingWsMessage(
            IncomingWsMessageType.AddInstance,
            instanceCreation
          )
          val resultMsg = OutgoingWsMessage(
            OutgoingWsMessageType.InstanceCreationSuccessMsg,
            InstanceCreationSuccess(
              instanceCreation,
              instanceWithStatus
            )
          )
          incoming.feed(Json.toJson(creationMsg)(incomingWsMessagesWrites)).end
          verify(controller.webSocketService).send(id,
                                                   Json.toJson(resultMsg)(OutgoingWsMessage.outgoingWsMessageWrites))
      }
    }

    "process instance addition correctly" in new WithApplication {
      val instanceCreation = InstanceCreation(
        "template",
        Map(
          "id" -> "blib"
        )
      )

      val success = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceCreationSuccessMsg,
        InstanceCreationSuccess(
          instanceCreation,
          instanceWithStatus
        )
      )
      val roleFailure = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceCreationFailureMsg,
        InstanceCreationFailure(
          instanceCreation,
          "Only administrators are allowed to create new instances"
        )
      )
      val regexFailure = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceCreationFailureMsg,
        InstanceCreationFailure(
          instanceCreation,
          "Only allowed to create instances matching bla"
        )
      )

      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock(classOf[WebSocketService]),
            templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
            instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
            aboutService = withDummyValues(mock(classOf[AboutInfoService])),
            securityService = securityService
          )
          when(controller.instanceService.addInstance(instanceCreation)).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingWsMessage(
          IncomingWsMessageType.AddInstance,
          instanceCreation
        ),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> regexFailure,
          Some((".*", Role.Operator)) -> roleFailure,
          Some((".*", Role.NormalUser)) -> roleFailure
        )
      )
    }

    "process instance deletion correctly" in new WithApplication {
      val instanceDeletion = "id"

      val success = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceDeletionSuccessMsg,
        InstanceDeletionSuccess(
          instanceDeletion,
          instanceWithStatus
        )
      )
      val roleFailure = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceDeletionFailureMsg,
        InstanceDeletionFailure(
          instanceDeletion,
          "Only administrators are allowed to delete instances"
        )
      )
      val regexFailure = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceDeletionFailureMsg,
        InstanceDeletionFailure(
          instanceDeletion,
          "Only allowed to delete instances matching bla"
        )
      )

      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock(classOf[WebSocketService]),
            templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
            instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
            aboutService = withDummyValues(mock(classOf[AboutInfoService])),
            securityService = securityService
          )
          when(controller.instanceService.deleteInstance(instanceDeletion)).thenReturn(Success(instanceWithStatus))
          controller
        },
        inMsg = IncomingWsMessage(
          IncomingWsMessageType.DeleteInstance,
          instanceDeletion
        ),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> regexFailure,
          Some((".*", Role.Operator)) -> roleFailure,
          Some((".*", Role.NormalUser)) -> roleFailure
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

      val success = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceUpdateSuccessMsg,
        InstanceUpdateSuccess(
          instanceUpdate,
          instanceWithStatus
        )
      )
      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock(classOf[WebSocketService]),
            templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
            instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
            aboutService = withDummyValues(mock(classOf[AboutInfoService])),
            securityService = securityService
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
        inMsg = IncomingWsMessage(
          IncomingWsMessageType.UpdateInstance,
          instanceUpdate
        ),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only allowed to update instances matching bla"
            )
          ),
          Some((".*", Role.Operator)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Updating parameter values or templates only allowed for administrators."
            )
          ),
          Some((".*", Role.NormalUser)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only administrators and operators are allowed to update instances"
            )
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

      val success = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceUpdateSuccessMsg,
        InstanceUpdateSuccess(
          instanceUpdate,
          instanceWithStatus
        )
      )
      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock(classOf[WebSocketService]),
            templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
            instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
            aboutService = withDummyValues(mock(classOf[AboutInfoService])),
            securityService = securityService
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
        inMsg = IncomingWsMessage(
          IncomingWsMessageType.UpdateInstance,
          instanceUpdate
        ),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only allowed to update instances matching bla"
            )
          ),
          Some((".*", Role.Operator)) -> success,
          Some((".*", Role.NormalUser)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only administrators and operators are allowed to update instances"
            )
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

      val success = OutgoingWsMessage(
        OutgoingWsMessageType.InstanceUpdateSuccessMsg,
        InstanceUpdateSuccess(
          instanceUpdate,
          instanceWithStatus
        )
      )
      testWs(
        controllerSetup = { securityService =>
          val controller = WebSocketController(
            webSocketService = mock(classOf[WebSocketService]),
            templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
            instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
            aboutService = withDummyValues(mock(classOf[AboutInfoService])),
            securityService = securityService
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
        inMsg = IncomingWsMessage(
          IncomingWsMessageType.UpdateInstance,
          instanceUpdate
        ),
        expectations = Map(
          None -> success,
          Some((".*", Role.Administrator)) -> success,
          Some(("bla", Role.Administrator)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only allowed to update instances matching bla"
            )
          ),
          Some((".*", Role.Operator)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Updating parameter values or templates only allowed for administrators."
            )
          ),
          Some((".*", Role.NormalUser)) -> OutgoingWsMessage(
            OutgoingWsMessageType.InstanceUpdateFailureMsg,
            InstanceUpdateFailure(
              instanceUpdate,
              "Only administrators and operators are allowed to update instances"
            )
          )
        )
      )
    }

  }

}
