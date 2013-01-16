/*
 * Copyright (C) 2011-2012 scalapenos.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scalapenos.riak

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import akka.actor._


/**
 * This test depends on a Riak node running on localhost:8098 !!
 */
class BasicInteractionsSpec extends AkkaActorSystemSpecification {
  val timeout = 5 seconds

  import converters.BasicRiakValueConverters._

  "The riak client" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = RiakClient(system)
      val connection = client.connect()
      val bucket = connection.bucket("test-basic-interaction")

      val fetchBeforeStore = bucket.fetch("foo")

      Await.result(fetchBeforeStore, timeout) must beNone

      val store = bucket.store("foo", "bar")
      val storedValue = Await.result(store, timeout)

      storedValue must beSome[RiakValue]
      storedValue.get.value must beEqualTo("bar")

      val fetchAfterStore = bucket.fetch("foo")
      val fetchedValue = Await.result(fetchAfterStore, timeout)

      fetchedValue must beSome[RiakValue]
      fetchedValue.get.value must beEqualTo("bar")

      val delete = bucket.delete("foo")

      Await.result(delete, timeout) must beEqualTo(())

      val fetchAfterDelete = bucket.fetch("foo")

      Await.result(fetchAfterDelete, timeout) must beNone
    }
  }

}
