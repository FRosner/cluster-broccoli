package de.frosner.broccoli.nomad

import play.api.libs.ws.WSResponse

/**
  * An unexpected and unhandled response from the Nomad API.
  *
  * @param response The original response
  */
class UnexpectedNomadHttpApiError(val response: WSResponse)
    extends Exception(s"Unexpected Nomad response: ${response.status} ${response.statusText}") {}
