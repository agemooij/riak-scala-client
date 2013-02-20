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
  // TODO: tests for multiple indexes
  //       - mixed types
  //       - same name but different value
  //       - different names, same values


  import RiakSecondaryIndexesTestData._

  "A RiakBucket" should {
    "support storing and returning a value with one secondary Int index" in new
      StoreAndReturnBody[ClassWithOneIntIndex](ClassWithOneIntIndex("bar"), "bar", ClassWithOneIntIndex.indexes) {}

    "support storing a value with one secondary Int index and then fetching it by key" in new
      StoreAndFetch[ClassWithOneIntIndex](ClassWithOneIntIndex("bar"), "bar", ClassWithOneIntIndex.indexes) {}

    "support storing and returning a value with one secondary String index" in new
      StoreAndReturnBody[ClassWithOneStringIndex](ClassWithOneStringIndex("bar"), "bar", ClassWithOneStringIndex.indexes) {}

    "support storing a value with one secondary String index and then fetching it by key" in new
      StoreAndFetch[ClassWithOneStringIndex](ClassWithOneStringIndex("bar"), "bar", ClassWithOneStringIndex.indexes) {}

    "support storing and returning a value with two secondary Int indexes" in new
      StoreAndReturnBody[ClassWithTwoIntIndexes](ClassWithTwoIntIndexes("bar"), "bar", ClassWithTwoIntIndexes.indexes) {}

    "support storing and returning a value with two secondary Int indexes with the same index name" in new
      StoreAndReturnBody[ClassWithTwoIntIndexesWithTheSameName](ClassWithTwoIntIndexesWithTheSameName("bar"),
                                                                "bar",
                                                                ClassWithTwoIntIndexesWithTheSameName.indexes) {}

    "support storing and returning a value with multiple mixed indexes, including double names" in new
      StoreAndReturnBody[ClassWithMixedIndexes](ClassWithMixedIndexes("bar"), "bar", ClassWithMixedIndexes.indexes) {}

    "support storing and returning a value with String indexes that contain one or more commas" in new
      StoreAndReturnBody[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces](
        ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces("bar"),
        "bar",
        ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces.indexes) {}

    "support storing a value with multiple mixed indexes and then fetching it using each index" in new
      StoreAndFetchByIndexes[ClassWithMixedIndexes](ClassWithMixedIndexes("bar"), "bar", ClassWithMixedIndexes.indexes) {}
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

    bucket.delete(key).await must beEqualTo(())
    bucket.fetch(key).await must beNone
  }

  abstract class StoreAndFetchByIndexes[T: RiakSerializer: RiakIndexer](constructor: => T, expectedData: String, expectedIndexes: Set[RiakIndex]) extends Scope {
    val bucket = connection.bucket("riak-index-fetching-tests")
    val key = randomKey
    val value = constructor

    bucket.fetch(key).await must beNone
    bucket.store(key, value).await

    expectedIndexes.foreach { expectedIndex =>
      val fetchedValues = bucket.fetch(expectedIndex).await

      fetchedValues must have size(1)
      fetchedValues.head.data must beEqualTo(expectedData)
      fetchedValues.head.indexes must beEqualTo(expectedIndexes)
    }

    bucket.delete(key).await must beEqualTo(())
    bucket.fetch(key).await must beNone
  }
}
