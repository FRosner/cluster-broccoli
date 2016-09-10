package de.frosner.broccoli.controllers

import de.frosner.broccoli.models.Template
import de.frosner.broccoli.services.TemplateService
import org.specs2.mutable.Specification
import play.api.test.{FakeRequest, PlaySpecification}
import org.mockito.Mockito._
import play.api.libs.json.{JsArray, JsObject, JsString}
import org.specs2.concurrent.ExecutionEnv
import play.api.mvc.BodyParsers

class TemplateControllerSpec extends PlaySpecification {

  "list templates" should {

    "list all available templates" in { implicit ee: ExecutionEnv =>
      val templateService = mock(classOf[TemplateService])
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description"
      )
      when(templateService.templates).thenReturn(Seq(template))
      val controller = new TemplateController(templateService)

      val result = controller.list.apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsArray(Seq(
        JsObject(Map(
          "id" -> JsString(template.id),
          "parameterNames" -> JsArray(Seq(JsString("id"))),
          "description" -> JsString(template.description),
          "version" -> JsString(template.templateVersion)
        ))
      ))
    }

  }

  "show template" should {

    "return the template if it exists" in { implicit ee: ExecutionEnv =>
      val templateService = mock(classOf[TemplateService])
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description"
      )
      when(templateService.template("id")).thenReturn(Some(template))
      val controller = new TemplateController(templateService)

      val result = controller.show("id").apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsObject(Map(
        "id" -> JsString(template.id),
        "parameterNames" -> JsArray(Seq(JsString("id"))),
        "description" -> JsString(template.description),
        "version" -> JsString(template.templateVersion)
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
