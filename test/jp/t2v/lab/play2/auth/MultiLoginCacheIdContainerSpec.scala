package jp.t2v.lab.play2.auth

import play.api.cache.Cache
import play.api.test.{PlaySpecification, WithApplication}

class MultiLoginCacheIdContainerSpec extends PlaySpecification {

  sequential

  val container = new MultiLoginCacheIdContainer[String]

  "starting a new session" should {

    "work" in new WithApplication {
      val user = "user"
      val token = container.startNewSession(user, 1000)
      Cache.get(token + container.tokenSuffix) === Some(user)
    }

    "time out" in new WithApplication {
      val token = container.startNewSession("user", 1)
      Thread.sleep(2000)
      Cache.get(token + container.tokenSuffix) should beNone
    }

  }

  "removing a session" should {

    "work" in new WithApplication {
      val user = "user"
      val token = container.startNewSession(user, 1000)
      container.remove(token)
      Cache.get(token + container.tokenSuffix) should beNone
    }

  }

  "getting a session" should {

    "work" in new WithApplication {
      val user = "user"
      val token = container.startNewSession(user, 1000)
      container.get(token) === Some(user)
    }

  }

  "prolonging a session timeout" should {

    "work" in new WithApplication {
      val user = "user"
      val token = container.startNewSession(user, 1)
      container.prolongTimeout(token, 100)
      Thread.sleep(2000)
      Cache.get(token + container.tokenSuffix) === Some(user)
    }

  }

}
