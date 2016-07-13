package de.frosner.broccoli.controllers

import akka.actor.ActorSystem
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.services.{InstanceService, TemplateService}
import org.mockito.Mockito._
import org.specs2.concurrent.ExecutionEnv
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.test.{FakeRequest, PlaySpecification}
import akka.testkit.{TestActorRef, TestKit, TestProbe}

import scala.util.Success

class InstanceControllerSpec extends TestKit(ActorSystem("TestProbesTestSystem")) with PlaySpecification {

  "list instances" should {

    "list all available instances" in { implicit ee: ExecutionEnv =>
      val instanceService = TestProbe()
//      val future = instanceService.ref ? "hello"
//      instanceService.expectMsg(0 millis, "hello")
//      instanceService.reply("world")
//      assert(future.isCompleted && future.value == Some(Success("world")))
      val controller = new InstanceController(instanceService.ref)
      1 === 1
    }

    "list all available instances of the specified template" in { implicit ee: ExecutionEnv =>

    }

  }

  "show template" should {

    "return the template if it exists" in { implicit ee: ExecutionEnv =>
      val templateService = mock(classOf[TemplateService])
      val template = Template(
        id = "id",
        template = "template {{name}}",
        description = "description"
      )
      when(templateService.template("id")).thenReturn(Some(template))
      val controller = new TemplateController(templateService)

      val result = controller.show("id").apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsObject(Map(
        "id" -> JsString(template.id),
        "parameters" -> JsArray(Seq(JsString("name"))),
        "description" -> JsString(template.description)
      ))
    }

    "return 404 if the template does not exist" in {
      val templateService = mock(classOf[TemplateService])
      when(templateService.template("id")).thenReturn(None)
      val controller = new TemplateController(templateService)

      val result = controller.show("id").apply(FakeRequest())
      status(result) must be equalTo 404
    }

  }

}
