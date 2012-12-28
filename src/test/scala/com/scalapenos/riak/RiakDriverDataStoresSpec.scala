package com.scalapenos.riak

import akka.actor._
import scala.concurrent.duration._

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import spray.json._

import domain._
import DataStore._

class RiakDriverDataStoresSpec extends Specification with NoTimeConversions {
  val random = new util.Random()
  def randomUserId = random.nextLong().abs.toString

  "The BlockingJavaRiakDriverDataStore" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in
      new BasicCrudOperationSpecs(Props[BlockingJavaRiakDriverDataStore])
  }

  "The BlockingJavaProtobufRiakDriverDataStore" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in
      new BasicCrudOperationSpecs(Props[BlockingJavaProtobufRiakDriverDataStore])
  }

  "The NonBlockingJavaRiakDriverDataStore" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in
      new BasicCrudOperationSpecs(Props[NonBlockingJavaRiakDriverDataStore])
  }

  "The NonBlockingJavaProtobufRiakDriverDataStore" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in
      new BasicCrudOperationSpecs(Props[NonBlockingJavaProtobufRiakDriverDataStore])
  }

  "The SprayClientRiakDriverDataStore" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in
      new BasicCrudOperationSpecs(Props[SprayClientRiakDriverDataStore])
  }

  private class BasicCrudOperationSpecs(actorProps: Props) extends AkkaTestkitContext {
    within(1 seconds) {
      val datastore = system.actorOf(actorProps)
      val route = NavRoute(Some(Location(2.001, 42)))
      val userId = randomUserId

      datastore ! FindActiveRoute(userId)

      expectMsgType[FoundActiveRoute] mustEqual FoundActiveRoute(userId, None)

      datastore ! UpdateActiveRoute(userId, route)

      expectMsgType[UpdatedActiveRoute] mustEqual UpdatedActiveRoute(userId, route)

      datastore ! FindActiveRoute(userId)

      expectMsgType[FoundActiveRoute] mustEqual FoundActiveRoute(userId, Some(route))

      datastore ! DeleteActiveRouteFor(userId)

      expectMsgType[DeletedActiveRouteFor] mustEqual DeletedActiveRouteFor(userId)

      datastore ! FindActiveRoute(userId)

      expectMsgType[FoundActiveRoute] mustEqual FoundActiveRoute(userId, None)

      expectNoMsg()
    }
  }

}
