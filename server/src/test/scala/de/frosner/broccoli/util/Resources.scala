package de.frosner.broccoli.util

import scala.io.Source

/**
  * Utilities for reading resources.
  */
object Resources {

  /**
    * Read a resource as string.
    *
    * @param path The resource path
    * @return The resource string
    */
  def readAsString(path: String): String = {
    val stream = getClass.getResourceAsStream(path)
    try {
      Source.fromInputStream(stream, "UTF-8").mkString
    } finally {
      stream.close()
    }
  }
}
