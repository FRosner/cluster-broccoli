package de.frosner.broccoli.controllers

import de.frosner.broccoli.auth.Account
import de.frosner.broccoli.models._
import de.frosner.broccoli.services.{SecurityService, TemplateService}
import de.frosner.broccoli.templates.TemplateConfiguration
import de.frosner.broccoli.templates.jinjava.JinjavaConfiguration
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.test.Helpers.stubControllerComponents

import scala.concurrent.ExecutionContext

class TemplateControllerSpec extends PlaySpecification with AuthUtils {

  sequential // http://stackoverflow.com/questions/31041842/error-with-play-2-4-tests-the-cachemanager-has-been-shut-down-it-can-no-longe

  val templateConfig = TemplateConfiguration("/dummy/path/to/templates", JinjavaConfiguration(), "TOKEN")

  "list" should {

    "list all available templates" in new WithApplication {
      val template = Template(
        id = "id",
        template = "template {{id}}",
        description = "description",
        parameterInfos = Map(
          "id" -> ParameterInfo(id = "id",
                                name = Some("myname"),
                                default = Some(StringParameterValue("myid")),
                                secret = Some(false),
                                `type` = ParameterType.String,
                                orderIndex = None)
        )
      )

      testWithAllAuths { (securityService, account) =>
        TemplateController(
          withTemplates(mock[TemplateService], List(template)),
          templateConfig,
          securityService,
          playEnv,
          cacheApi,
          stubControllerComponents(),
          ExecutionContext.global,
          withIdentities(account)
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
        parameterInfos = Map(
          "id" -> ParameterInfo(id = "id",
                                name = Some("myname"),
                                default = Some(StringParameterValue("myid")),
                                secret = Some(false),
                                `type` = ParameterType.String,
                                orderIndex = None)
        )
      )

      testWithAllAuths { (securityService, account) =>
        TemplateController(
          withTemplates(mock[TemplateService], List(template)),
          templateConfig,
          securityService,
          playEnv,
          cacheApi,
          stubControllerComponents(),
          ExecutionContext.global,
          withIdentities(account)
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

      testWithAllAuths { (securityService, account) =>
        TemplateController(
          templateService,
          templateConfig,
          securityService,
          playEnv,
          cacheApi,
          stubControllerComponents(),
          ExecutionContext.global,
          withIdentities(account)
        )
      } { controller =>
        controller.show("id")
      }(_.withBody(())) { (controller, result) =>
        status(result) must be equalTo 404
      }
    }

  }

  "refresh" should {
    val template1 = Template(
      id = "id1",
      template = "template1 {{id}}",
      description = "description",
      parameterInfos = Map(
        "id" -> ParameterInfo(id = "id",
                              name = Some("myname"),
                              default = Some(StringParameterValue("myid")),
                              secret = Some(false),
                              `type` = ParameterType.String,
                              orderIndex = None)
      )
    )

    val template2 = Template(
      id = "id2",
      template = "template2 {{id}}",
      description = "description",
      parameterInfos = Map(
        "id" -> ParameterInfo(id = "id",
                              name = Some("myname"),
                              default = Some(StringParameterValue("myid")),
                              secret = Some(false),
                              `type` = ParameterType.String,
                              orderIndex = None)
      )
    )
    val templateService = mock[TemplateService]
    when(templateService.getTemplates(false)).thenReturn(Seq(template1))
    when(templateService.getTemplates(true)).thenReturn(Seq(template1, template2))

    "reload the templates if auth token is correct" in new WithApplication {
      val controller = TemplateController(
        templateService,
        templateConfig,
        withAuthNone(mock[SecurityService]),
        playEnv,
        cacheApi,
        stubControllerComponents(),
        ExecutionContext.global,
        withIdentities(Account.anonymous)
      )
      val result = controller.refresh(FakeRequest().withBody(RefreshRequest("TOKEN", true)))
      status(result) must be equalTo 200 and {
        contentAsJson(result).as[Seq[Template]].size must be equalTo 2
      }
    }

    "401 if auth token is incorrect" in new WithApplication {
      val controller = TemplateController(
        templateService,
        templateConfig,
        withAuthNone(mock[SecurityService]),
        playEnv,
        cacheApi,
        stubControllerComponents(),
        ExecutionContext.global,
        withIdentities(Account.anonymous)
      )
      val result = controller.refresh(FakeRequest().withBody(RefreshRequest("BADTOKEN", true)))
      status(result) must be equalTo UNAUTHORIZED
    }
  }

}
