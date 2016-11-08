package de.frosner.broccoli.services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{Instance, InstanceStatus, Template}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.Configuration

import scala.util.{Failure, Success, Try}

class InstanceStorageSpec extends Specification {

  "Any instance storage" should {

    def testStorage = new InstanceStorage {
      protected override def readInstancesImpl(): Try[Set[Instance]] = Failure(new Exception())
      override def readInstancesImpl(idFilter: (String) => Boolean): Try[Set[Instance]] = Failure(new Exception())
      override def readInstanceImpl(id: String): Try[Instance] = Failure(new Exception())
      override def deleteInstanceImpl(toDelete: Instance): Try[Instance] = Failure(new Exception())
      override def writeInstanceImpl(instance: Instance): Try[Instance] = Failure(new Exception())
      override def closeImpl(): Unit = {}
      override val prefix: String = "prefix"
      def testSuccess = {
        val expected = "blub"
        checkPrefix("prefix-bla"){Success(expected)} === Success(expected)
      }
      def testFailure = {
        val expected = "blub"
        checkPrefix("bla"){Success(expected)} should beFailedTry
      }
    }

    "succeed if the prefix matches" in {
      testStorage.testSuccess
    }

    "fail if the prefix does not match" in {
      testStorage.testFailure
    }

    "be closed after closing" in {
      val storage = testStorage
      storage.close()
      storage.isClosed === true
    }

    "not be closed before closing" in {
      testStorage.isClosed === false
    }

    "should not allow readInstances if closed" in {
      val storage = testStorage
      storage.close()
      storage.readInstances should throwA[IllegalStateException]
    }

    "should not allow readInstances(filter) if closed" in {
      val storage = testStorage
      storage.close()
      storage.readInstances(_ => true) should throwA[IllegalStateException]
    }

    "should not allow readInstance if closed" in {
      val storage = testStorage
      storage.close()
      storage.readInstance("id") should throwA[IllegalStateException]
    }

    "should not allow deleteInstance if closed" in {
      val storage = testStorage
      storage.close()
      storage.deleteInstance(null) should throwA[IllegalStateException]
    }

    "should not allow writeInstance if closed" in {
      val storage = testStorage
      storage.close()
      storage.writeInstance(null) should throwA[IllegalStateException]
    }

  }

}
