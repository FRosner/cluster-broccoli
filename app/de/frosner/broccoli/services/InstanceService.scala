package de.frosner.broccoli.services

import javax.inject.{Singleton, Inject}

import de.frosner.broccoli.models.{InstanceStatus, InstanceCreation, Instance}
import de.frosner.broccoli.util.Logging
import play.api.{Logger, Configuration}
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}

@Singleton
class InstanceService @Inject() (configuration: Configuration, ws: WSClient, templateService: TemplateService) extends Logging {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val nomadBaseUrl = configuration.getString("broccoli.nomad.url").getOrElse("http://localhost:4646")
  private val nomadJobPrefix = configuration.getString("broccoli.nomad.jobPrefix").getOrElse("")

  // TODO regularily ask nomad for updates on instance status instead of with every GET, do this with actors?
  private var instances = Map(
    "zeppelin-frank" -> Instance(
      id = "zeppelin-frank",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "frank"),
      actualStatus = InstanceStatus.Pending,
      desiredStatus = InstanceStatus.Running
    ),
    "zeppelin-pauline" -> Instance(
      id = "zeppelin-pauline",
      template = templateService.template("zeppelin").get,
      parameterValues = Map("id" -> "pauline"),
      actualStatus = InstanceStatus.Running,
      desiredStatus = InstanceStatus.Running
    ),
    "jupyter-basil" -> Instance(
      id = "jupyter-basil",
      template = templateService.template("jupyter").get,
      parameterValues = Map("id" -> "basil"),
      actualStatus = InstanceStatus.Stopped,
      desiredStatus = InstanceStatus.Stopped
    )
  )

  def getInstances: Iterable[Instance] = instances.values

  def getInstance(id: String): Option[Instance] = instances.get(id)

  def addInstance(instanceCreation: InstanceCreation): Try[String] = synchronized {
    Logger.info(s"Request received to create new instance: $instanceCreation")
    val maybeId = instanceCreation.parameters.get("id")
    val templateId = instanceCreation.templateId
    maybeId.map { id =>
      if (instances.contains(id)) {
        Failure(newExceptionWithWarning(new IllegalArgumentException(s"There is already an instance having the ID $id")))
      } else {
        val potentialTemplate = templateService.template(templateId)
        potentialTemplate.map { template =>
          instances = instances.updated(id, Instance(id, template, instanceCreation.parameters, InstanceStatus.Unknown, InstanceStatus.Unknown))
          Success(id)
        }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException(s"Template $templateId does not exist."))))
      }
    }.getOrElse(Failure(newExceptionWithWarning(new IllegalArgumentException("No ID specified"))))
  }

  // TODO when do I ask for the status of the instance? if asking, I need to ask Nomad
  // I guess each instance should have a status object that is queried only if GET /instances/<id> is used

  def nomadInstances: Future[Seq[Instance]] = {
    val jobsRequest = ws.url(nomadBaseUrl + "/v1/jobs").withQueryString("prefix" -> nomadJobPrefix)
    val jobsResponse = jobsRequest.get().map(_.json.as[JsArray])
    val jobsWithTemplate = jobsResponse.map(jsArray => {
      val (ids, names) = ((jsArray \\ "ID").map(_.as[JsString].value), (jsArray \\ "Name").map(_.as[JsString].value))
      ids.zip(names).flatMap{
        case (id, name) => templateService.template(name).map(
          template => Instance(id, template, Map("id" -> id), InstanceStatus.Unknown, InstanceStatus.Unknown)
        )
      }
    })
    jobsWithTemplate
  }

  def nomadInstance(id: String): Future[Option[Instance]] = nomadInstances.map(_.find(_.id == id))

}
