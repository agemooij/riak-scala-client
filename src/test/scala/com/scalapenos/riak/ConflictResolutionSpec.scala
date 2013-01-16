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
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.util._

import akka.actor._


/**
 * This test depends on a Riak node running on localhost:8098 !!
 */
class ConflictResolutionSpec extends AkkaActorSystemSpecification {
  val timeout = 5 seconds

  import converters.BasicRiakValueConverters._

  "When resolving conflicts with a custom resolver during fetch, the client" should {
    "write the resolved value back to Riak and return the new value with the appropriate vector clock" in {
      val resolver = new ConflictResolver {
        def resolve(values: Set[RiakValue]) = {
          println("=====> " + values.map(_.value))

          values.find(v => v.value == "foo").getOrElse(values.head)
        }
      }

      val bucket = RiakClient(system).connect().bucket(name = "test-conflict-resolution", resolver = resolver)

      val stored1 = Await.result(bucket.store("foo", "bar"), timeout)
      val stored2 = Await.result(bucket.store("foo", "foo"), timeout)
      val stored3 = Await.result(bucket.store("foo", "baz"), timeout)

      val fetched = Await.result(bucket.fetch("foo"), timeout)

      fetched must beSome[RiakValue]
      fetched.get.value must beEqualTo("foo")



      val delete = Await.result(bucket.delete("foo"), timeout)

      val fetchAfterDelete = bucket.fetch("foo")

      Await.result(fetchAfterDelete, timeout) must beNone
    }
  }

}
