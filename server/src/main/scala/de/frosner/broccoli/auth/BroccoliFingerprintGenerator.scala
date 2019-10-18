package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.crypto.Hash
import com.mohiva.play.silhouette.api.util.FingerprintGenerator
import play.api.http.HeaderNames.USER_AGENT
import play.api.mvc.RequestHeader

case class BroccoliFingerprintGenerator(includeRemoteAddress: Boolean = false) extends FingerprintGenerator {
  override def generate(implicit request: RequestHeader): String =
    Hash.sha1(
      new StringBuilder()
        .append(request.headers.get(USER_AGENT).getOrElse(""))
        .append(":")
        .append(if (includeRemoteAddress) request.remoteAddress else "")
        .toString())
}
