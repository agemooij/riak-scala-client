package com.scalapenos.riak

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._


class RiakDriverDataStoresSpec extends AkkaActorSystemSpecification {

  "The riak driver" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = Riak(system)
      val connection = client.connect()
      val bucket = connection.bucket("test")

      Await.result(bucket.fetch("foo"), 5 seconds) must beNone
    }
  }

}
