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


  case class ClassWithOneIntIndex(foo: String)
  object ClassWithOneIntIndex {
    implicit def serializer1 = new RiakSerializer[ClassWithOneIntIndex] {
      def serialize(iv: ClassWithOneIntIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer1 = new RiakDeserializer[ClassWithOneIntIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneIntIndex] = Success(apply(data))
    }

    implicit def indexer1 = new RiakIndexer[ClassWithOneIntIndex] {
      def index(v: ClassWithOneIntIndex) = Set(RiakIndex("foos", 1))
    }
  }

  case class ClassWithOneStringIndex(foo: String)
  object ClassWithOneStringIndex {
    implicit def serializer2 = new RiakSerializer[ClassWithOneStringIndex] {
      def serialize(iv: ClassWithOneStringIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer2 = new RiakDeserializer[ClassWithOneStringIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneStringIndex] = Success(apply(data))
    }

    implicit def indexer2 = new RiakIndexer[ClassWithOneStringIndex] {
      def index(v: ClassWithOneStringIndex) = Set(RiakIndex("foos", v.foo))
    }
  }

  "A RiakBucket" should {
    "support storing and returning a value with one secondary index (Int)" in {
      val bucket = connection.bucket("riak-index-tests")
      val key = randomKey
      val value = ClassWithOneIntIndex("bar")

      bucket.fetch(key).await must beNone

      val storedValue = bucket.store(key, value, true).await

      storedValue must beSome[RiakValue]
      storedValue.get.data must beEqualTo("bar")
      storedValue.get.indexes must have size(1)
      storedValue.get.indexes.head must beEqualTo(RiakIndex("foos", 1))

      val meta = storedValue.get.toMeta[ClassWithOneIntIndex]

      meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneIntIndex]]]
      meta.get.indexes must have size(1)
      meta.get.indexes.head must beEqualTo(RiakIndex("foos", 1))

      bucket.delete(key).await must beEqualTo(())
      bucket.fetch(key).await must beNone
    }

    "support storing and then fetching a value with one secondary index (Int)" in {
      val bucket = connection.bucket("riak-index-tests")
      val key = randomKey
      val value = ClassWithOneIntIndex("bar")

      bucket.fetch(key).await must beNone
      bucket.store(key, value).await

      val fetchedValue = bucket.fetch(key).await

      fetchedValue must beSome[RiakValue]
      fetchedValue.get.data must beEqualTo("bar")
      fetchedValue.get.indexes must have size(1)
      fetchedValue.get.indexes.head must beEqualTo(RiakIndex("foos", 1))

      val meta = fetchedValue.get.toMeta[ClassWithOneIntIndex]

      meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneIntIndex]]]
      meta.get.indexes must have size(1)
      meta.get.indexes.head must beEqualTo(RiakIndex("foos", 1))

      bucket.delete(key).await must beEqualTo(())
      bucket.fetch(key).await must beNone
    }

    "support storing and returning a value with one secondary index (String)" in {
      val bucket = connection.bucket("riak-index-tests")
      val key = randomKey
      val value = ClassWithOneStringIndex("bar")

      bucket.fetch(key).await must beNone

      val storedValue = bucket.store(key, value, true).await

      storedValue must beSome[RiakValue]
      storedValue.get.data must beEqualTo("bar")
      storedValue.get.indexes must have size(1)
      storedValue.get.indexes.head must beEqualTo(RiakIndex("foos", "bar"))

      val meta = storedValue.get.toMeta[ClassWithOneStringIndex]

      meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneStringIndex]]]
      meta.get.indexes must have size(1)
      meta.get.indexes.head must beEqualTo(RiakIndex("foos", "bar"))

      bucket.delete(key).await must beEqualTo(())
      bucket.fetch(key).await must beNone
    }

    "support storing and then fetching a value with one secondary index (String)" in {
      val bucket = connection.bucket("riak-index-tests")
      val key = randomKey
      val value = ClassWithOneStringIndex("bar")

      bucket.fetch(key).await must beNone
      bucket.store(key, value).await

      val fetchedValue = bucket.fetch(key).await

      fetchedValue must beSome[RiakValue]
      fetchedValue.get.data must beEqualTo("bar")
      fetchedValue.get.indexes must have size(1)
      fetchedValue.get.indexes.head must beEqualTo(RiakIndex("foos", "bar"))

      val meta = fetchedValue.get.toMeta[ClassWithOneStringIndex]

      meta must beAnInstanceOf[Success[RiakMeta[ClassWithOneStringIndex]]]
      meta.get.indexes must have size(1)
      meta.get.indexes.head must beEqualTo(RiakIndex("foos", "bar"))

      bucket.delete(key).await must beEqualTo(())
      bucket.fetch(key).await must beNone
    }
  }

}
