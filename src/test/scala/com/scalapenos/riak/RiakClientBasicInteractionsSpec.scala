package com.scalapenos.riak

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import akka.actor._


/**
 * This test depends on a Riak node running on localhost:8098 !!
 */
class RiakClientBasicInteractionsSpec extends AkkaActorSystemSpecification {
  val timeout = 5 seconds

  "The riak driver" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = Riak(system)
      val connection = client.connect()
      val bucket = connection.bucket("test-" + Random.nextLong)

      val fetchBeforeStore = bucket.fetch("foo")

      Await.result(fetchBeforeStore, timeout) must beNone

      val store = bucket.store("foo", "bar")
      val storedValue = Await.result(store, timeout)

      storedValue must beSome[RiakValue]
      storedValue.get.asString must beEqualTo("bar")

      val fetchAfterStore = bucket.fetch("foo")
      val fetchedValue = Await.result(fetchAfterStore, timeout)

      fetchedValue must beSome[RiakValue]
      fetchedValue.get.asString must beEqualTo("bar")

      val delete = bucket.delete("foo")

      Await.result(delete, timeout) must beEqualTo(())

      val fetchAfterDelete = bucket.fetch("foo")

      Await.result(fetchAfterDelete, timeout) must beNone
    }
  }

}
