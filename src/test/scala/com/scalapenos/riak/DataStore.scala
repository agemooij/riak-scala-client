package com.scalapenos.riak

import domain._

object DataStore {
  case class FindActiveRoute(userId: String)
  case class FoundActiveRoute(userId: String, route: Option[NavRoute])

  case class UpdateActiveRoute(userId: String, route: NavRoute)
  case class UpdatedActiveRoute(userId: String, route: NavRoute)

  case class DeleteActiveRouteFor(userId: String)
  case class DeletedActiveRouteFor(userId: String)

  case class DataStoreError(problem: Throwable)
}
