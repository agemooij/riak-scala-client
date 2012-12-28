package com.scalapenos.riak
package domain

import spray.json.DefaultJsonProtocol._


case class Location(lat: Double, long: Double)
object Location {
  implicit val jsonFormat = jsonFormat2(Location.apply)
}

case class NavRoute(destination: Option[Location])
object NavRoute {
  val Empty = NavRoute(None)

  implicit val jsonFormat = jsonFormat1(NavRoute.apply)
}

case class User(id: String, activeRoute: Option[NavRoute])
object User {
  implicit val jsonFormat = jsonFormat2(User.apply)
}

