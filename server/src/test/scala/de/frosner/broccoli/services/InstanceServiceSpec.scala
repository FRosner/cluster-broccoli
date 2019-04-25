package de.frosner.broccoli.services

import com.hubspot.jinjava.JinjavaConfig
import de.frosner.broccoli.controllers.ServiceMocks
import de.frosner.broccoli.instances.storage.InstanceStorage
import de.frosner.broccoli.models._
import de.frosner.broccoli.nomad.{NomadClient, NomadConfiguration}
import de.frosner.broccoli.templates.TemplateRenderer
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

class InstanceServiceSpec extends Specification with Mockito with ServiceMocks {
  val templateRenderer =
    new TemplateRenderer(JinjavaConfig.newBuilder().withFailOnUnknownTokens(true).build())

  implicit val nomadConfiguration: NomadConfiguration =
    NomadConfiguration(url = s"http://localhost:4646", "NOMAD_BROCCOLI_TOKEN", namespacesEnabled = false, "oe")

  val service = new InstanceService(
    nomadClient = mock[NomadClient],
    nomadService = mock[NomadService],
    templateService = withTemplates(mock[TemplateService], List.empty),
    consulService = mock[ConsulService],
    applicationLifecycle = mock[ApplicationLifecycle],
    templateRenderer = templateRenderer,
    instanceStorage = mock[InstanceStorage],
    config = Configuration.from(Map("broccoli.polling.frequency" -> 1))
  )

  "Updating the parameters of an instance" should {

    "work correctly" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                               "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      )
      val newParameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("30"))
      val newInstance = service.updateParameterValues(instance, newParameterValues)
      newInstance.get.parameterValues === newParameterValues
    }

    "not allow changing of the ID" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                               "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      )
      val newParameterValues = Map("id" -> RawParameterValue("2"), "age" -> RawParameterValue("40"))
      val newInstance = service.updateParameterValues(instance, newParameterValues)
      newInstance.isFailure === true
    }

    "require parameter and value consistency" in {
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                               "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
        ),
        parameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      )
      val newParameterValues = Map("id" -> RawParameterValue("1"))
      val newInstance = service.updateParameterValues(instance, newParameterValues)
      newInstance.isFailure === true
    }
  }

  "Updating the template of an instance" should {
    "work correctly when the parameters don't change" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                             "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                             "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
      )
      val originalParameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = service.updateTemplate(instance, newTemplate, newParameterValues)
      (newInstance.get.template === newTemplate) and (newInstance.get.parameterValues === newParameterValues)
    }

    "work correctly when the parameters change" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                             "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map(
          "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
          "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None),
          "height" -> ParameterInfo("height", None, None, None, ParameterType.Raw, None)
        )
      )
      val originalParameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      val newParameterValues = originalParameterValues.updated("height", RawParameterValue("170"))
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = service.updateTemplate(instance, newTemplate, newParameterValues)
      (newInstance.get.template === newTemplate) and (newInstance.get.parameterValues === newParameterValues)
    }

    "require parameter and value consistency" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                             "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map(
          "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
          "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None),
          "height" -> ParameterInfo("height", None, None, None, ParameterType.Raw, None)
        )
      )
      val originalParameterValues = Map("id" -> RawParameterValue("1"), "age" -> RawParameterValue("50"))
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = service.updateTemplate(instance, newTemplate, newParameterValues)
      (newInstance.isFailure === true) and (instance.template === originalTemplate)
    }

    "not allow changing of the ID" in {
      val originalTemplate = Template(
        id = "1",
        template = "\"{{id}} {{age}}\"",
        description = "desc",
        parameterInfos = Map("id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
                             "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None))
      )
      val newTemplate = Template(
        id = "5",
        template = "\"{{id}} {{age}} {{height}}\"",
        description = "desc",
        parameterInfos = Map(
          "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
          "age" -> ParameterInfo("age", None, None, None, ParameterType.Raw, None),
          "height" -> ParameterInfo("height", None, None, None, ParameterType.Raw, None)
        )
      )
      val originalParameterValues = Map("id" -> RawParameterValue("2"), "age" -> RawParameterValue("50"))
      val newParameterValues = originalParameterValues
      val instance = Instance(
        id = "1",
        template = originalTemplate,
        parameterValues = originalParameterValues
      )
      val newInstance = service.updateTemplate(instance, newTemplate, newParameterValues)
      newInstance.isFailure === true
    }
  }

}
