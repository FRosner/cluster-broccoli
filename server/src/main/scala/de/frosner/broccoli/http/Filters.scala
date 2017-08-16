package de.frosner.broccoli.http

import javax.inject.Inject

import play.api.http.DefaultHttpFilters

/**
  * Provide all HTTP filters for Broccoli.
  *
  * @param accessControlFilter A filter to setup access control for Broccoli
  */
class Filters @Inject()(accessControlFilter: AccessControlFilter) extends DefaultHttpFilters(accessControlFilter) {}
