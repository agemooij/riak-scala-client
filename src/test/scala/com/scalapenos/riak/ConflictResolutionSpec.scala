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
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.util._

import akka.actor._

import spray.util._


class ConflictResolutionSpec extends AkkaActorSystemSpecification {

  "When resolving conflicts with a custom resolver during fetch, the client" should {
    "write the resolved value back to Riak and return the new value with the appropriate vector clock" in {
      val resolver = new ConflictResolver {
        def resolve(values: Set[RiakValue]) = {
          values.find(v => v.data == "foo").getOrElse(failTest("The resolver should always find the value 'foo'."))
        }
      }

      val bucket = RiakClient(system).connect().bucket(name = "test-conflict-resolution", resolver = resolver)

      val stored1 = bucket.store("foo", "bar").await
      val stored2 = bucket.store("foo", "foo").await
      val stored3 = bucket.store("foo", "baz").await

      val fetched = bucket.fetch("foo").await

      fetched must beSome[RiakValue]
      fetched.get.data must beEqualTo("foo")

      bucket.delete("foo").await

      val fetchAfterDelete = bucket.fetch("foo").await

      fetchAfterDelete must beNone
    }
  }

}
