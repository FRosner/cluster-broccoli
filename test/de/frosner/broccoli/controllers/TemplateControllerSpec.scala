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

  "/templates" should {

    "list all available templates" in { implicit ee: ExecutionEnv =>
      val templateService = mock(classOf[TemplateService])
      val template = Template(
        id = "id",
        template = "template {{name}}",
        description = "description"
      )
      when(templateService.templates).thenReturn(Seq(template))
      val controller = new TemplateController(templateService)
      
      val result = controller.list.apply(FakeRequest())
      status(result) must be equalTo 200
      contentAsJson(result) must be equalTo JsArray(Seq(
        JsObject(Map(
          "id" -> JsString(template.id),
          "parameters" -> JsArray(Seq(JsString("name"))),
          "description" -> JsString(template.description)
        ))
      ))
    }

  }

}
