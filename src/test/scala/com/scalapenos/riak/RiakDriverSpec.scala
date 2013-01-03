package com.scalapenos.riak

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._

import spray.json._


class RiakDriverDataStoresSpec extends AkkaActorSystemSpecification {
  case class Foo(bar: String)
  object Foo extends DefaultJsonProtocol {
    implicit val jsonFormat = jsonFormat1(Foo.apply)
  }

  "The riak driver" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = Riak(system)
      val connection = client.connect()
      val bucket = connection.bucket[Foo]("test")

      Await.result(bucket.fetch("foo"), 5 seconds) must beEqualTo(FetchKeyNotFound)
    }
  }

}
