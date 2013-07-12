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

import java.util.UUID._


class RiakSecondaryIndexesSpec extends RiakClientSpecification with RandomKeySupport {
  // TODO: skip this test if the localhost backend does not support secondary indexes

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

    "support storing and fetching a value with multiple mixed indexes" in new
      StoreAndFetch[ClassWithMixedIndexes](ClassWithMixedIndexes("bar"), "bar", ClassWithMixedIndexes.indexes) {}

    "support storing multiple key/value pairs with the same index and fetching them by that index" in {
      val numbers = (0 to 10).toList

      val intIndex = 42
      val intIndexKeys = numbers.map(n => "int-indexed-" + n)
      val intIndexValues  = numbers.map(n => ClassWithConfigurableIntIndex("foo-int-" + n, intIndex))
      val intIndexKvs = intIndexKeys.zip(intIndexValues)

      val stringIndex = "foo"
      val stringIndexKeys = numbers.map(n => "string-indexed-" + n)
      val stringIndexValues = numbers.map(n => ClassWithConfigurableStringIndex("foo-string-" + n, stringIndex))
      val stringIndexKvs = stringIndexKeys.zip(stringIndexValues)

      val bucket = client.bucket("riak-index-tests-" + randomKey)
      val storedValues1 = Future.traverse(intIndexKvs)(kv => bucket.storeAndFetch(kv._1, kv._2)).await
      val storedValues2 = Future.traverse(stringIndexKvs)(kv => bucket.storeAndFetch(kv._1, kv._2)).await

      storedValues1 must have size(numbers.size)
      storedValues2 must have size(numbers.size)

      bucket.fetch(ClassWithConfigurableIntIndex.indexName, intIndex).await must have size(numbers.size)
      bucket.fetch(ClassWithConfigurableStringIndex.indexName, stringIndex).await must have size(numbers.size)

      Future.traverse(intIndexKeys ++ stringIndexKeys)(bucket.delete(_)).await must have size(numbers.size * 2)
    }

    "support storing multiple key/value pairs with int indexes and fetching multiple values using index ranges" in {
      val indexName = ClassWithConfigurableIntIndex.indexName
      val indexes = (0 to 10).toList ++ (40 to 60).toList
      val keys = indexes.map(n => "key-" + n)
      val values = indexes.map(n => ClassWithConfigurableIntIndex("foo-" + n, n))
      val kvs = keys.zip(values)

      val bucket = client.bucket("riak-index-tests-" + randomKey)
      val storedValues = Future.traverse(kvs)(kv => bucket.storeAndFetch(kv._1, kv._2)).await

      storedValues must have size(indexes.size)

      bucket.fetch(indexName,  0, 60).await must have size(indexes.size)
      bucket.fetch(indexName,  60, 0).await must have size(indexes.size)
      bucket.fetch(indexName,  0, 80).await must have size(indexes.size)
      bucket.fetch(indexName,-10, 80).await must have size(indexes.size)
      bucket.fetch(indexName,  0, 10).await must have size(11)
      bucket.fetch(indexName, 11, 39).await must have size(0)
      bucket.fetch(indexName, 10, 40).await must have size(2)
      bucket.fetch(indexName,  0,  0).await must have size(1)
      bucket.fetch(indexName,  0,  1).await must have size(2)

      bucket.fetch(indexName, 30).await must have size(0)

      Future.traverse(keys)(bucket.delete(_)).await must have size(indexes.size)
    }

    "support storing multiple key/value pairs with String indexes and fetching multiple values using index ranges" in {
      val indexName = ClassWithConfigurableStringIndex.indexName
      val numberIndexes = ((0 to 10).toList ++ (40 to 60).toList).map(_.toString)
      val letterIndexes = List("a", "b", "y", "z", "aa", "bb", "yy", "zz", "A", "B", "Y", "Z")
      val indexes = numberIndexes ++ letterIndexes
      val keys = indexes.map(n => "key-" + n)
      val values = indexes.map(n => ClassWithConfigurableStringIndex("foo-" + n, n))
      val kvs = keys.zip(values)

      val bucket = client.bucket("riak-index-tests-" + randomKey)
      val storedValues = Future.traverse(kvs)(kv => bucket.storeAndFetch(kv._1, kv._2)).await

      storedValues must have size(indexes.size)

      bucket.fetch(indexName,  "0",   "9").await must have size(numberIndexes.size)
      bucket.fetch(indexName,  "9",   "0").await must have size(numberIndexes.size)
      bucket.fetch(indexName,  "0",  "90").await must have size(numberIndexes.size)
      bucket.fetch(indexName, "00",  "90").await must have size(numberIndexes.size - 1)
      bucket.fetch(indexName,  "0",  "zz").await must have size(indexes.size)
      bucket.fetch(indexName,  "0", "zza").await must have size(indexes.size)
      bucket.fetch(indexName, "zz",   "0").await must have size(indexes.size)
      bucket.fetch(indexName,  "a",  "zz").await must have size(8)
      bucket.fetch(indexName,  "a",   "z").await must have size(7)
      bucket.fetch(indexName, "aa",  "zz").await must have size(7)
      bucket.fetch(indexName,  "a",   "a").await must have size(1)
      bucket.fetch(indexName,  "a",   "b").await must have size(3)
      bucket.fetch(indexName, "aa",   "b").await must have size(2)
      bucket.fetch(indexName,  "A",   "B").await must have size(2)

      Future.traverse(keys)(bucket.delete(_)).await must have size(indexes.size)
    }
  }


  // ==========================================================================
  // Misc Helpers
  // ==========================================================================

  import org.specs2.specification.Scope

  abstract class StoreAndFetch[T: RiakMarshaller](constructor: => T, expectedData: String, expectedIndexes: Set[RiakIndex]) extends Scope {
    val key = randomKey
    val value = constructor
    val bucket = client.bucket("riak-index-tests-" + key)

    bucket.fetch(key).await must beNone

    val storedValue = bucket.storeAndFetch(key, value).await

    storedValue.data must beEqualTo(expectedData)
    storedValue.indexes must beEqualTo(expectedIndexes)

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
