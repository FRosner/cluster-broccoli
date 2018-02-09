package de.frosner.broccoli.instances

case class PeriodicJobNotFoundException(instanceId: String, jobId: String)
    extends Exception(s"Periodic job '$jobId' not found for instance '$instanceId'")
