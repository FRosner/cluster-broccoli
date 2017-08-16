package jp.t2v.lab.play2.auth

import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import play.api.cache.CacheApi

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Random

class MultiLoginCacheIdContainer[Id: ClassTag](cache: CacheApi) extends IdContainer[Id] {

  private val log = play.api.Logger(getClass)

  private[auth] val tokenSuffix = ":multitoken"
  private[auth] val random = new Random(new SecureRandom())

  override def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    log.info(s"Starting new session for user '$userId'.")
    val token = generate
    store(token, userId, Duration(timeoutInSeconds.toLong, TimeUnit.SECONDS))
    token
  }

  @tailrec
  private[auth] final def generate: AuthenticityToken = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
    val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (get(token).isDefined) generate else token
  }

  def remove(token: AuthenticityToken) {
    log.info(s"Deleting session of user '${get(token)}'")
    cache.remove(token + tokenSuffix)
  }

  def get(token: AuthenticityToken): Option[Id] =
    cache.get(token + tokenSuffix).map(_.asInstanceOf[Id])

  private[auth] def store(token: AuthenticityToken, userId: Id, duration: Duration) {
    cache.set(token + tokenSuffix, userId, duration)
  }

  override def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) {
    get(token).foreach(store(token, _, Duration(timeoutInSeconds.toLong, TimeUnit.SECONDS)))
  }

}
