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

import scala.util.Success

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

  private def wrap(messageType: IncomingWsMessageType, payload: JsValue): JsValue = JsObject(Map(
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

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "WebSocketController" should {

    "establish a websocket connection correctly (with authentication)" in new WithWebSocketApplication[Msg] {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
      )
      val result = controller.requestToSocket(FakeRequest().withLoggedIn(controller)(account.name))
      val maybeConnection = wrapConnection(result)
      maybeConnection should beRight
    }

    "establish a websocket connection correctly (without authentication)" in new WithWebSocketApplication[Msg] {
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
      val maybeConnection = wrapConnection(result)
      maybeConnection should beRight
    }

    "decline the websocket connection if not authenticated" in new WithWebSocketApplication[Msg] {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
      val controller = WebSocketController(
        webSocketService = mock(classOf[WebSocketService]),
        templateService = withTemplates(mock(classOf[TemplateService]), Seq.empty),
        instanceService = withInstances(mock(classOf[InstanceService]), Seq.empty),
        aboutService = withDummyValues(mock(classOf[AboutInfoService])),
        securityService = withAuthConf(mock(classOf[SecurityService]), List(account))
      )
      val result = controller.requestToSocket(FakeRequest())
      val maybeConnection = wrapConnection(result)
      maybeConnection should beLeft.like {
        case d => d.header.status === 403
      }
    }

    "send about info, template and instance list after establishing the connection" in new WithWebSocketApplication[Msg] {
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
      val maybeConnection = wrapConnection(result)
      maybeConnection should beRight.like {
        case (incoming, outgoing) =>
          val messages = outgoing.get
          (messages should haveSize(3)) and
            (messages should contain(
              Json.toJson(OutgoingWsMessage(OutgoingWsMessageType.ListTemplatesMsg, templates)),
              Json.toJson(OutgoingWsMessage(OutgoingWsMessageType.ListInstancesMsg, instances)),
              Json.toJson(OutgoingWsMessage(OutgoingWsMessageType.AboutInfoMsg, controller.aboutService.aboutInfo(null)))
            ))
      }
    }

    "process instance addition requests from admins" in new WithWebSocketApplication[Msg] {
      val account = UserAccount("user", "pass", ".*", Role.Administrator)
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
      val maybeConnection = wrapConnection(result)
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
          verify(controller.webSocketService).send(id, Json.toJson(resultMsg)(OutgoingWsMessage.outgoingWsMessageWrites))
      }
    }

    "propagate other problems during instance creation" in new WithWebSocketApplication[Msg] {

    }

    "not process instance addition requests from operators" in new WithApplication {

    }

    "not process instance addition requests from users" in new WithApplication {

    }

    "process instance parameter updates from admins" in new WithApplication {

    }

    "not process instance parameter updates from operators" in new WithApplication {

    }

    "not process instance parameter updates from users" in new WithApplication {

    }

    "process instance template updates from admins" in new WithApplication {

    }

    "not process instance template updates from operators" in new WithApplication {

    }

    "not process instance template updates from users" in new WithApplication {

    }

    "process instance status change requests from admins" in new WithApplication {

    }

    "process instance status change requests from operators" in new WithApplication {

    }

    "not process instance status change requests from users" in new WithApplication {

    }

    "process instance deletions from admins" in new WithApplication {

    }

    "not process instance deletions from operators" in new WithApplication {

    }

    "not process instance deletions from users" in new WithApplication {

    }

    "ignore unknown incoming messages" in new WithApplication {

    }

  }

}
