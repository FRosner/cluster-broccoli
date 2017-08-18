package jp.t2v.lab.play2.auth

import play.api.Application
import play.api.cache.{Cache, CacheApi}
import play.api.test.{PlaySpecification, WithApplication}

class MultiLoginCacheIdContainerSpec extends PlaySpecification {

  sequential

  def cache(implicit app: Application): CacheApi = app.injector.instanceOf[CacheApi]

  "starting a new session" should {

    "work" in new WithApplication {
      val container = new MultiLoginCacheIdContainer[String](cache)

      val user = "user"
      val token = container.startNewSession(user, 1000)
      cache.get(token + container.tokenSuffix) must beSome(beEqualTo(user))
    }

    "time out" in new WithApplication {
      val container = new MultiLoginCacheIdContainer[String](cache)

      val token = container.startNewSession("user", 1)
      Thread.sleep(2000)
      cache.get(token + container.tokenSuffix) should beNone
    }

  }

  "removing a session" should {

    "work" in new WithApplication {
      val container = new MultiLoginCacheIdContainer[String](cache)

      val user = "user"
      val token = container.startNewSession(user, 1000)
      container.remove(token)
      cache.get(token + container.tokenSuffix) should beNone
    }

  }

  "getting a session" should {

    "work" in new WithApplication {
      val container = new MultiLoginCacheIdContainer[String](cache)

      val user = "user"
      val token = container.startNewSession(user, 1000)
      container.get(token) === Some(user)
    }

  }

  "prolonging a session timeout" should {

    "work" in new WithApplication {
      val container = new MultiLoginCacheIdContainer[String](cache)

      val user = "user"
      val token = container.startNewSession(user, 1)
      container.prolongTimeout(token, 100)
      Thread.sleep(2000)
      cache.get(token + container.tokenSuffix) === Some(user)
    }

  }

}
