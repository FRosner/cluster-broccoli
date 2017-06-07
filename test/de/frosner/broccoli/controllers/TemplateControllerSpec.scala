package de.frosner.broccoli.controllers

import de.frosner.broccoli.models.{ParameterInfo, Template}
import de.frosner.broccoli.services.{SecurityService, TemplateService}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import org.mockito.Mockito._
import play.api.libs.json._
import org.specs2.concurrent.ExecutionEnv

class TemplateControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  "list" should {

    "list all available templates" in new WithApplication {
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description",
        parameterInfos = Map(
          "id" -> ParameterInfo(id = "id", name = Some("myname"), default = Some("myid"), secret = Some(false))
        )
      )

      testWithAllAuths { securityService =>
          TemplateController(
            templateService = withTemplates(mock(classOf[TemplateService]), List(template)),
            securityService = securityService
          )
      } { controller =>
        controller.list
      } {
        identity
      } { (controller, result) =>
        (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsArray(Seq(
            JsObject(Map(
              "id" -> JsString(template.id),
              "parameters" -> JsArray(Seq(JsString("id"))),
              "parameterInfos" -> JsObject(Map(
                "id" -> JsObject(Map(
                  "id" -> JsString("id"),
                  "name" -> JsString("myname"),
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
    }
  }

  "show" should {

    "return the template if it exists" in new WithApplication {
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description",
        parameterInfos = Map.empty
      )

      testWithAllAuths { securityService =>
        TemplateController(
          templateService = withTemplates(mock(classOf[TemplateService]), List(template)),
          securityService = securityService
        )
      } { controller =>
        controller.show("id")
      } {
        identity
      } { (controller, result) =>
        (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsObject(Map(
            "id" -> JsString(template.id),
            "parameters" -> JsArray(Seq(JsString("id"))),
            "parameterInfos" -> JsObject(Map.empty[String, JsValue]),
            "description" -> JsString(template.description),
            "version" -> JsString(template.version)
          ))
        }
      }
    }

    "return 404 if the template does not exist" in new WithApplication {
      val templateService = mock(classOf[TemplateService])
      when(templateService.template("id")).thenReturn(None)

      testWithAllAuths { securityService =>
        TemplateController(
          templateService = templateService,
          securityService = securityService
        )
      } { controller =>
        controller.show("id")
      } {
        identity
      } { (controller, result) =>
        status(result) must be equalTo 404
      }
    }

  }

}
