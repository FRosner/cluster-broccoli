package de.frosner.broccoli.controllers

import de.frosner.broccoli.models._
import de.frosner.broccoli.services.{TemplateService}
import play.api.test.{PlaySpecification, WithApplication}
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
        documentation_url = "#documentation_url",
        parameterInfos = Map(
          "id" -> ParameterInfo(id = "id",
                                name = Some("myname"),
                                default = Some(StringParameterValue("myid")),
                                secret = Some(false),
                                `type` = ParameterType.String,
                                orderIndex = None)
        )
      )

      testWithAllAuths { securityService =>
        TemplateController(
          withTemplates(mock[TemplateService], List(template)),
          securityService,
          playEnv,
          cacheApi
        )
      } { controller =>
        controller.list
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsArray(
            Seq(
              JsObject(Map(
                "id" -> JsString(template.id),
                "parameters" -> JsArray(Seq(JsString("id"))),
                "parameterInfos" -> JsObject(Map(
                  "id" -> JsObject(Map(
                    "id" -> JsString("id"),
                    "name" -> JsString("myname"),
                    "default" -> JsString("myid"),
                    "secret" -> JsBoolean(false),
                    "type" -> JsObject(Map("name" -> JsString("string")))
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
        documentation_url = "#documentation_url",
        parameterInfos = Map(
          "id" -> ParameterInfo(id = "id",
                                name = Some("myname"),
                                default = Some(StringParameterValue("myid")),
                                secret = Some(false),
                                `type` = ParameterType.String,
                                orderIndex = None)
        )
      )

      testWithAllAuths { securityService =>
        TemplateController(
          withTemplates(mock[TemplateService], List(template)),
          securityService,
          playEnv,
          cacheApi
        )
      } { controller =>
        controller.show("id")
      }(_.withBody(())) { (controller, result) =>
        (status(result) must be equalTo 200) and {
          contentAsJson(result) must be equalTo JsObject(
            Map(
              "id" -> JsString(template.id),
              "parameters" -> JsArray(Seq(JsString("id"))),
              "parameterInfos" -> JsObject(Map(
                "id" -> JsObject(Map(
                  "id" -> JsString("id"),
                  "name" -> JsString("myname"),
                  "default" -> JsString("myid"),
                  "secret" -> JsBoolean(false),
                  "type" -> JsObject(Map("name" -> JsString("string")))
                ))
              )),
              "description" -> JsString(template.description),
              "version" -> JsString(template.version)
            ))
        }
      }
    }

    "return 404 if the template does not exist" in new WithApplication {
      val templateService = mock[TemplateService]
      when(templateService.template("id")).thenReturn(None)

      testWithAllAuths { securityService =>
        TemplateController(
          templateService,
          securityService,
          playEnv,
          cacheApi
        )
      } { controller =>
        controller.show("id")
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 404
      }
    }

  }

}
