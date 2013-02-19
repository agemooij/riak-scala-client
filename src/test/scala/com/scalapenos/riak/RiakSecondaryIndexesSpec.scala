/*
 * Copyright (C) 2012-2013 Age Mooij
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

import java.util.UUID._

import akka.actor._

import spray.util._

/**
 * This test depends on a Riak node running on localhost:8098 !!
 */
class RiakSecondaryIndexesSpec extends RiakClientSpecification with RandomKeySupport {

  // TODO: skip this test if the localhost backend does not support secondary indexes
  // TODO: get rid of all the duplication
  // TODO: tests for multiple indexes
  //       - mixed types
  //       - same name but different value
  //       - different names, same values


  import test.TestClassesWithIndexes._

  "A RiakBucket" should {
    "support storing and returning a value with one secondary index (Int)" in new
      StoreAndReturnBody[ClassWithOneIntIndex](ClassWithOneIntIndex("bar"), "bar", Set(RiakIndex("foos", 1))) {}

    "support storing and returning a value with one secondary index (String)" in new
      StoreAndReturnBody[ClassWithOneStringIndex](ClassWithOneStringIndex("bar"), "bar", Set(RiakIndex("foos", "bar"))) {}

    "support storing and then fetching a value with one secondary index (Int)" in new
      StoreAndFetch[ClassWithOneIntIndex](ClassWithOneIntIndex("bar"), "bar", Set(RiakIndex("foos", 1))) {}

    "support storing and then fetching a value with one secondary index (String)" in new
      StoreAndFetch[ClassWithOneStringIndex](ClassWithOneStringIndex("bar"), "bar", Set(RiakIndex("foos", "bar"))) {}
  }


  // ==========================================================================
  // Misc Helpers
  // ==========================================================================

  import org.specs2.specification.Scope

  abstract class StoreAndReturnBody[T: RiakSerializer: RiakIndexer](constructor: => T, expectedData: String, expectedIndexes: Set[RiakIndex]) extends Scope {
    val bucket = connection.bucket("riak-index-tests")
    val key = randomKey
    val value = constructor

    bucket.fetch(key).await must beNone

    val storedValue = bucket.store(key, value, true).await

    storedValue must beSome[RiakValue]
    storedValue.get.data must beEqualTo(expectedData)
    storedValue.get.indexes must beEqualTo(expectedIndexes)

    val meta = storedValue.get.toMeta[ClassWithOneStringIndex]

    meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneStringIndex]]]
    meta.get.indexes must beEqualTo(expectedIndexes)

    bucket.delete(key).await must beEqualTo(())
    bucket.fetch(key).await must beNone
  }

  abstract class StoreAndFetch[T: RiakSerializer: RiakIndexer](constructor: => T, expectedData: String, expectedIndexes: Set[RiakIndex]) extends Scope {
    val bucket = connection.bucket("riak-index-tests")
    val key = randomKey
    val value = constructor

    bucket.fetch(key).await must beNone
    bucket.store(key, value).await

    val fetchedValue = bucket.fetch(key).await

    fetchedValue must beSome[RiakValue]
    fetchedValue.get.data must beEqualTo(expectedData)
    fetchedValue.get.indexes must beEqualTo(expectedIndexes)

    val meta = fetchedValue.get.toMeta[ClassWithOneStringIndex]

    meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneStringIndex]]]
    meta.get.indexes must beEqualTo(expectedIndexes)

    bucket.delete(key).await must beEqualTo(())
    bucket.fetch(key).await must beNone
  }
}
