package de.frosner.broccoli.services

import java.io.{File, FileNotFoundException, FileOutputStream, PrintStream}
import java.net.ConnectException
import java.nio.file.Paths
import java.util.UUID

import de.frosner.broccoli.models.{Instance, Template}
import Instance.{instancePersistenceWrites, instancePersistenceReads}
import org.specs2.mutable.Specification

import play.core.server.Server
import play.api.Play
import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._

import scala.io.Source
import scala.util.{Failure, Success, Try}


class CouchDBInstanceStorageSpec extends Specification {

  val instance = Instance(
    id = "prefix-id",
    template = Template(id = "t", template = "{{id}}" ,description = "d", parameterInfos = Map.empty),
    parameterValues = Map(
      "id" -> "prefix-id"
    )
  )

  "Creating the CouchDB instance storage" should {

    "succeed if the database already exists" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client) should beAnInstanceOf[CouchDBInstanceStorage]
        }
      }
    }

    "succeed if the database does not exist but can be created" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.NotFound
        }
        case PUT(p"/broccoli_instances") => Action {
          Results.Created
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client) should beAnInstanceOf[CouchDBInstanceStorage]
        }
      }
    }

    "fail if the database does not exist but cannot be created" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.NotFound
        }
        case PUT(p"/broccoli_instances") => Action {
          Results.InternalServerError
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client) should throwA[IllegalArgumentException]
        }
      }
    }

    "fail if the database cannot be reached" in {
      WsTestClient.withClient { client =>
        CouchDBInstanceStorage(s"http://localhost:43643", "", client) should throwA[ConnectException]
      }
    }

  }

  "Reading all instances" should {

    "work" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/_all_docs") => Action {
          val docJson = JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id)))
          Results.Ok(
            Json.obj(
              "offset" -> 0,
              "rows" -> Json.arr(
                Json.obj(
                  "doc" -> docJson
                )
              )
            )
          )
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstances() === Success(Set(instance))
        }
      }
    }

    "filter instances not matching the prefix" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/_all_docs") => Action {
          val docJson = JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id)))
          Results.Ok(
            Json.obj(
              "offset" -> 0,
              "rows" -> Json.arr(
                Json.obj(
                  "doc" -> docJson
                )
              )
            )
          )
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "notmatching", client)
          storage.readInstances() === Success(Set.empty[Instance])
        }
      }
    }

    "filter instances not matching the additional filter" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/_all_docs") => Action {
          val docJson = JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id)))
          Results.Ok(
            Json.obj(
              "offset" -> 0,
              "rows" -> Json.arr(
                Json.obj(
                  "doc" -> docJson
                )
              )
            )
          )
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstances(_ == "notexistingdsds") === Success(Set.empty[Instance])
        }
      }
    }

    "fail if an instance ID does not match the document ID" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/_all_docs") => Action {
          val docJson = JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id + "xxx")))
          Results.Ok(
            Json.obj(
              "offset" -> 0,
              "rows" -> Json.arr(
                Json.obj(
                  "doc" -> docJson
                )
              )
            )
          )
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstances().failed.get should beAnInstanceOf[IllegalStateException]
        }
      }
    }

    "fail if _all_docs is not reachable" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/_all_docs") => Action {
          Results.NotFound
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstances().isFailure === true
        }
      }
    }

  }

  /*
  {
    "_id": "blub",
    "_rev": "1-653239dcce0d790983a29cfc907f6d2b",
    "id": "blub"
  }
   */
  "Reading an instance" should {

    "work" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstance(instance.id) === Success(instance)
        }
      }
    }

    "fail if the prefix does not match" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "notmatching", client)
          storage.readInstance(instance.id).failed.get should beAnInstanceOf[PrefixViolationException]
        }
      }
    }

    "fail if the instance does not exist" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.NotFound
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstance(instance.id) === Failure(InstanceNotFoundException(instance.id))
        }
      }
    }

    "fail if the document is not a valid instance JSON" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id)).-("template")))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstance(instance.id).failed.get should beAnInstanceOf[JsResultException]
        }
      }
    }

    "fail if the instance ID does not match the document ID" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_id", JsString(instance.id + "a"))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.readInstance(instance.id).failed.get should beAnInstanceOf[IllegalStateException]
        }
      }
    }

  }

  "Writing an instance" should {

    "work for newly created instances" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.NotFound
        }
        case PUT(p"/broccoli_instances/prefix-id") => Action {
          Results.Created
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.writeInstance(instance) === Success(instance)
        }
      }
    }

    "work for instances overwriting another one" in {
      val revision = "1-revision"
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_rev", JsString(revision))))
        }
        case PUT(p"/broccoli_instances/prefix-id") => Action { request =>
          require(request.body.asJson.get.as[JsObject].value("_rev") == JsString(revision), "Revision needs to be put as well.")
          Results.Created
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.writeInstance(instance) === Success(instance)
        }
      }
    }

    "fail if the prefix does not match" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "notmatching", client)
          storage.writeInstance(instance).failed.get should beAnInstanceOf[PrefixViolationException]
        }
      }
    }

    "fail if checking whether the instance exists fails (neither 404 nor 200)" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.InternalServerError
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.writeInstance(instance).failed.get should beAnInstanceOf[IllegalStateException]
        }
      }
    }

    "fail if writing the instance failed" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.NotFound
        }
        case PUT(p"/broccoli_instances/prefix-id") => Action {
          Results.InternalServerError
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.writeInstance(instance).failed.get should beAnInstanceOf[IllegalStateException]
        }
      }
    }

  }

  "Deleting an instance" should {

    "work" in {
      val revision = "1-revision"
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_rev", JsString(revision))))
        }
        case DELETE(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.deleteInstance(instance) === Success(instance)
        }
      }
    }

    "fail if the prefix does not match" in {
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "notMatching", client)
          storage.deleteInstance(instance).failed.get should beAnInstanceOf[PrefixViolationException]
        }
      }
    }

    "fail if deleting the instance failed" in {
      val revision = "1-revision"
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.Ok(JsObject(Json.toJson(instance).as[JsObject].value.updated("_rev", JsString(revision))))
        }
        case DELETE(p"/broccoli_instances/prefix-id") => Action {
          Results.InternalServerError
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.deleteInstance(instance).failed.get should beAnInstanceOf[IllegalStateException]
        }
      }
    }

    "fail if the instance does not exist" in {
      val revision = "1-revision"
      Server.withRouter() {
        case GET(p"/broccoli_instances") => Action {
          Results.Ok
        }
        case GET(p"/broccoli_instances/prefix-id") => Action {
          Results.NotFound
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val storage = CouchDBInstanceStorage(s"http://localhost:$port/broccoli_instances", "", client)
          storage.deleteInstance(instance).failed.get should beAnInstanceOf[InstanceNotFoundException]
        }
      }
    }

  }

}
