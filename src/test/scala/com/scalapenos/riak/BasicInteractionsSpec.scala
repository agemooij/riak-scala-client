/*
 * Copyright (C) 2012-2013 Age Mooij (http://scalapenos.com)
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

import spray.util._


class BasicInteractionsSpec extends AkkaActorSystemSpecification {
  "The riak client" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = RiakClient(system)
      val connection = client.connect()
      val bucket = connection.bucket("test-basic-interaction")

      val fetchBeforeStore = bucket.fetch("foo")

      fetchBeforeStore.await must beNone

      val storedValue = bucket.store("foo", "bar", true).await

      storedValue must beSome[RiakValue]
      storedValue.get.data must beEqualTo("bar")

      val fetchAfterStore = bucket.fetch("foo").await

      fetchAfterStore must beSome[RiakValue]
      fetchAfterStore.get.data must beEqualTo("bar")

      bucket.delete("foo").await must beEqualTo(())

      val fetchAfterDelete = bucket.fetch("foo").await

      fetchAfterDelete must beNone
    }
  }

}
