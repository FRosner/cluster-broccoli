package de.frosner.broccoli.nomad.models

/**
  * Attaches a nomad ID to arbitrary payload.
  *
  * @param jobId The nomad ID
  * @param payload The payload
  * @tparam T The type of the paylo
  */
final case class WithId[T](jobId: String, payload: T)
