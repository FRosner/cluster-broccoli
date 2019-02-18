package de.frosner.broccoli.auth

import com.mohiva.play.silhouette.api.{Environment, Silhouette}

trait BroccoliSecurity {
  type AuthenticityToken = String
  type SignedToken = String

  def silhouette: Silhouette[DefaultEnv]
  def env: Environment[DefaultEnv] = silhouette.env

}
