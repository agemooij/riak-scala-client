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
    "support storing and fetching a value with one Int index (by key and each index)" in new
      StoreAndFetch[ClassWithOneIntIndex](ClassWithOneIntIndex("bar"), "bar", ClassWithOneIntIndex.indexes) {}

    "support storing and fetching a value with one String index (by key and each index)" in new
      StoreAndFetch[ClassWithOneStringIndex](ClassWithOneStringIndex("bar"), "bar", ClassWithOneStringIndex.indexes) {}

    "support storing and fetching a value with two Int indexes (by key and each index)" in new
      StoreAndFetch[ClassWithTwoIntIndexes](ClassWithTwoIntIndexes("bar"), "bar", ClassWithTwoIntIndexes.indexes) {}

    "support storing and fetching a value with two Int indexes with the same index name (by key and each index)" in new
      StoreAndFetch[ClassWithTwoIntIndexesWithTheSameName](ClassWithTwoIntIndexesWithTheSameName("bar"), "bar", ClassWithTwoIntIndexesWithTheSameName.indexes) {}

    "support storing and fetching a value with multiple mixed indexes, including double names (by key and each index)" in new
      StoreAndFetch[ClassWithMixedIndexes](ClassWithMixedIndexes("bar"), "bar", ClassWithMixedIndexes.indexes) {}

    "support storing and fetching a value with String indexes that contain one or more commas (by key and each index)" in new
      StoreAndFetch[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces](
        ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces("bar"),
        "bar",
        ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces.indexes) {}

    "support storing and fetching a value with multiple mixed indexes (by key and each index)" in new
      StoreAndFetch[ClassWithMixedIndexes](ClassWithMixedIndexes("bar"), "bar", ClassWithMixedIndexes.indexes) {}
  }


  // ==========================================================================
  // Misc Helpers
  // ==========================================================================

  import org.specs2.specification.Scope

  abstract class StoreAndFetch[T: RiakSerializer: RiakIndexer](constructor: => T, expectedData: String, expectedIndexes: Set[RiakIndex]) extends Scope {
    val key = randomKey
    val value = constructor
    val bucket = connection.bucket("riak-index-tests-" + key)

    bucket.fetch(key).await must beNone

    val storedValue = bucket.store(key, value, true).await

    storedValue must beSome[RiakValue]
    storedValue.get.data must beEqualTo(expectedData)
    storedValue.get.indexes must beEqualTo(expectedIndexes)

    val fetchedByKey = bucket.fetch(key).await

    fetchedByKey must beSome[RiakValue]
    fetchedByKey.get.data must beEqualTo(expectedData)
    fetchedByKey.get.indexes must beEqualTo(expectedIndexes)

    expectedIndexes.foreach { expectedIndex =>
      val fetchedByIndex = bucket.fetch(expectedIndex).await

      fetchedByIndex must have size(1)
      fetchedByIndex.head.data must beEqualTo(expectedData)
      fetchedByIndex.head.indexes must beEqualTo(expectedIndexes)
    }

    bucket.delete(key).await must beEqualTo(())
    bucket.fetch(key).await must beNone
  }
}
