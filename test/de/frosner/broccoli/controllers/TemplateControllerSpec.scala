package de.frosner.broccoli.controllers

import de.frosner.broccoli.models.{ParameterInfo, Template}
import de.frosner.broccoli.services.TemplateService
import org.specs2.mutable.Specification
import play.api.test.{FakeRequest, PlaySpecification}
import org.mockito.Mockito._
import play.api.libs.json._
import org.specs2.concurrent.ExecutionEnv
import play.api.mvc.BodyParsers

class TemplateControllerSpec extends PlaySpecification {

  "list templates" should {

    "list all available templates" in { implicit ee: ExecutionEnv =>
      val templateService = mock(classOf[TemplateService])
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description",
        parameterInfos = Map(
          "id" -> ParameterInfo(name = "id", default = Some("myid"), secret = Some(false))
        )
      )
      when(templateService.templates).thenReturn(Seq(template))
      val controller = new TemplateController(templateService)

      val result = controller.list.apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsArray(Seq(
        JsObject(Map(
          "id" -> JsString(template.id),
          "parameters" -> JsArray(Seq(JsString("id"))),
          "parameterInfos" -> JsObject(Map(
            "id" -> JsObject(Map(
              "name" -> JsString("id"),
              "default" -> JsString("myid"),
              "secret" -> JsBoolean(false)
            ))
          )),
          "description" -> JsString(template.description),
          "version" -> JsString(template.version)
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
        description = "description",
        parameterInfos = Map.empty
      )
      when(templateService.template("id")).thenReturn(Some(template))
      val controller = new TemplateController(templateService)

      val result = controller.show("id").apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsObject(Map(
        "id" -> JsString(template.id),
        "parameters" -> JsArray(Seq(JsString("id"))),
        "parameterInfos" -> JsObject(Map.empty[String, JsValue]),
        "description" -> JsString(template.description),
        "version" -> JsString(template.version)
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
