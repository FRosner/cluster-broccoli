package de.frosner.broccoli.instances.storage

import de.frosner.broccoli.models._
import org.specs2.mutable.Specification
import play.api.Logger

import scala.util.{Failure, Try}

class InstanceStorageSpec extends Specification {

  "Any instance storage" should {

    def testStorage = new InstanceStorage {
      protected override val log = Logger(getClass)
      protected override def readInstancesImpl: Try[Set[Instance]] = Failure(new Exception())
      override def readInstancesImpl(idFilter: (String) => Boolean): Try[Set[Instance]] = Failure(new Exception())
      override def readInstanceImpl(id: String): Try[Instance] = Failure(new Exception())
      override def deleteInstanceImpl(toDelete: Instance): Try[Instance] = Failure(new Exception())
      override def writeInstanceImpl(instance: Instance): Try[Instance] = Failure(new Exception())
      override def closeImpl(): Unit = {}
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
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          documentation_url = "#documentation_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
            "age" -> ParameterInfo("age",
                                   None,
                                   None,
                                   secret = Some(false),
                                   `type` = ParameterType.Integer,
                                   orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> StringParameterValue("Frank"), "age" -> IntParameterValue(50))
      )
      storage.deleteInstance(instance) should throwA[IllegalStateException]
    }

    "should not allow writeInstance if closed" in {
      val storage = testStorage
      storage.close()
      val instance = Instance(
        id = "1",
        template = Template(
          id = "1",
          template = "\"{{id}} {{age}}\"",
          description = "desc",
          documentation_url = "#documentation_url",
          parameterInfos = Map(
            "id" -> ParameterInfo("id", None, None, None, ParameterType.Raw, None),
            "age" -> ParameterInfo("age",
                                   None,
                                   None,
                                   secret = Some(false),
                                   `type` = ParameterType.Integer,
                                   orderIndex = None)
          )
        ),
        parameterValues = Map("id" -> StringParameterValue("Frank"), "age" -> IntParameterValue(50))
      )
      storage.writeInstance(instance) should throwA[IllegalStateException]
    }

  }

}
