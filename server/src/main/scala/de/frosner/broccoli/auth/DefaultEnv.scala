package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

class DefaultEnv extends Env {

  /** Identity
    */
  type I = Account

  /** Authenticator used for identification.
    *  [[com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator]] could've also been used for REST.
    */
  type A = CookieAuthenticator
}
