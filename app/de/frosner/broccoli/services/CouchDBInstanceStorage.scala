package de.frosner.broccoli.services

import java.io._
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import de.frosner.broccoli.models.Instance
import play.api.Logger
import play.api.libs.json.{JsObject, _}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * Instance storage using CouchDB as a peristence layer.
  * On construction it is checking whether the instance DB exists and creating it if it doesn't.
  */
case class CouchDBInstanceStorage(dbUrlString: String, prefix: String, ws: WSClient) extends InstanceStorage {

  import Instance.{instancePersistenceReads, instancePersistenceWrites}

  {
    require(!dbUrlString.trim.isEmpty, "CouchDB HTTP API URL must not be empty.")
    val dbUrl = ws.url(s"$dbUrlString")
    val dbUri = dbUrl.uri
    val dbGet = Await.result(dbUrl.get(), Duration(2, TimeUnit.SECONDS))
    if (dbGet.status == 200) {
      // TODO #130 check that the result is a database and not just 200
      Logger.info(s"Using $dbUri to persist instances.")
    } else {
      val dbPut = Await.result(dbUrl.execute("PUT"), Duration(2, TimeUnit.SECONDS))
      if (dbPut.status == 201) {
        Logger.info(s"$dbUri did not exist, so I created it.")
      } else {
        throw new IllegalArgumentException(s"$dbUri did not exist but failed to create: HTTP status code ${dbPut.status}")
      }
    }
  }

  private def requireDocIdEqualToInstanceId[T](docId: JsValue, instanceId: JsValue)(f: => T): T = {
    if (docId != instanceId) {
      val error = s"Document ID ($docId) did not match the instance ID ($instanceId)."
      Logger.error(error)
      throw new IllegalStateException(error)
    } else {
      f
    }
  }

  override def closeImpl(): Unit = {
  }

  /*
  {
    "_id": "blub",
    "_rev": "1-653239dcce0d790983a29cfc907f6d2b",
    "id": "blub"
  }
   */
  override def readInstanceImpl(id: String): Try[Instance] = {
    checkPrefix(id) {
      Try {
        val instanceUrl = ws.url(s"$dbUrlString/$id")
        val instanceResult = Await.result(instanceUrl.get(), Duration(5, TimeUnit.SECONDS))
        if (instanceResult.status == 200) {
          val instanceFields = instanceResult.json.as[JsObject].value
          val docId = instanceFields("_id")
          val instanceId = instanceFields("id")
          println(instanceFields)
          requireDocIdEqualToInstanceId(docId, instanceId) {
            val publicFields = instanceFields.filter {
              case (key, value) => !key.startsWith("_")
            }
            JsObject(publicFields).as[Instance]
          }
        } else {
          throw new InstanceNotFoundException(id)
        }
      }
    }
  }

  protected override def readInstancesImpl: Try[Set[Instance]] = {
    readInstances(_ => true)
  }

  /*
  {
    "total_rows": 2,
    "offset": 0,
    "rows": [
      {
        "id": "blub",
        "key": "blub",
        "value": {
          "rev": "1-653239dcce0d790983a29cfc907f6d2b"
        },
        "doc": {
          "_id": "blub",
          "_rev": "1-653239dcce0d790983a29cfc907f6d2b",
          "id": "blub"
        }
      },
      {
        "id": "test",
        "key": "test",
        "value": {
          "rev": "1-09e9ebd9a59e92bad4b95b15609896ad"
        },
        "doc": {
          "_id": "test",
          "_rev": "1-09e9ebd9a59e92bad4b95b15609896ad",
          "id": "test"
        }
      }
    ]
  }
   */
  // TODO #131 /_find http://docs.couchdb.org/en/2.0.0/api/database/find.html#post--db-_find so we can let the DB filter
  override def readInstancesImpl(idFilter: String => Boolean): Try[Set[Instance]] = {
    Try {
      val allDocsUrl = ws.url(s"$dbUrlString/_all_docs?include_docs=true")
      val allDocsResult = Await.result(allDocsUrl.get(), Duration(5, TimeUnit.SECONDS))
      val allDocsJsObject = allDocsResult.json.as[JsObject].value
      if (allDocsJsObject("offset").as[JsNumber].value != 0) {
        // TODO offset can be used for pagination but we need all the data
        throw new IllegalStateException("Received positive offset so we are missing some data here.")
      } else {
        val rows = allDocsJsObject("rows").as[JsArray].value
        rows.map { row =>
          val doc = row.as[JsObject].value("doc").as[JsObject].value
          val docId = doc("_id")
          val instanceId = doc("id")
          requireDocIdEqualToInstanceId(docId, instanceId) {
            val publicFields = doc.filter {
              case (key, value) => !key.startsWith("_")
            }
            JsObject(publicFields).as[Instance]
          }
        }.toSet.filter(instance => instance.id.startsWith(prefix) && idFilter(instance.id))
      }
    }
  }

  override def writeInstanceImpl(instance: Instance): Try[Instance] = {
    val id = instance.id
    checkPrefix(id) {
      Try {
        val instanceUrl = ws.url(s"$dbUrlString/$id")
        val instanceResult = Await.result(instanceUrl.get(), Duration(5, TimeUnit.SECONDS))
        def checkStatusCode(status: Int) = {
          if (status == 201) {
            instance
          } else {
            throw new IllegalStateException(s"Persisting instance '$id' failed. HTTP status code: $status")
          }
        }
        instanceResult.status match {
          case 404 => {
            // Instance does not exist so it will be created
            val creationResult = Await.result(instanceUrl.put(Json.toJson(instance)), Duration(5, TimeUnit.SECONDS))
            checkStatusCode(creationResult.status)
          }
          case 200 => {
            // Instance already exists so we are overwriting
            val presentInstanceRevision = instanceResult.json.as[JsObject].value("_rev").as[JsString]
            val instanceJson = Json.toJson(instance).as[JsObject]
            val instanceJsonWithRev = JsObject(instanceJson.value.updated("_rev", presentInstanceRevision))
            val creationResult = Await.result(instanceUrl.put(instanceJsonWithRev), Duration(5, TimeUnit.SECONDS))
            checkStatusCode(creationResult.status)
          }
          case other => throw new IllegalStateException(s"Unexpected status code returned from ${instanceUrl.uri}: $other")
        }
      }
    }
  }

  override def deleteInstanceImpl(instance: Instance): Try[Instance] = {
    val id = instance.id
    checkPrefix(id) {
      Try {
        val readInstanceUrl = ws.url(s"$dbUrlString/$id")
        val instanceResult = Await.result(readInstanceUrl.get(), Duration(5, TimeUnit.SECONDS))
        def checkStatusCode(status: Int) = {
          if (status == 200) {
            instance
          } else {
            throw new IllegalStateException(s"Deleting instance '$id' failed. HTTP status code: $status")
          }
        }
        instanceResult.status match {
          case 404 => {
            throw new InstanceNotFoundException(id)
          }
          case 200 => {
            // Instance already exists so we are overwriting
            val presentInstanceRevision = instanceResult.json.as[JsObject].value("_rev").as[JsString]
            val deleteInstanceUrl = ws.url(s"$dbUrlString/$id?rev=${presentInstanceRevision.value}")
            val creationResult = Await.result(deleteInstanceUrl.delete(), Duration(5, TimeUnit.SECONDS))
            checkStatusCode(creationResult.status)
          }
          case other => throw new IllegalStateException(s"Unexpected status code returned from ${readInstanceUrl.uri}: $other")
        }
      }
    }
  }

}
