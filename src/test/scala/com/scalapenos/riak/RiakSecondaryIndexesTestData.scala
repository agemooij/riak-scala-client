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


object RiakSecondaryIndexesTestData {
  import scala.util._

  case class ClassWithOneIntIndex(foo: String)
  object ClassWithOneIntIndex {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1))

    implicit def serializer = new RiakSerializer[ClassWithOneIntIndex] {
      def serialize(iv: ClassWithOneIntIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithOneIntIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneIntIndex] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithOneIntIndex] {
      def index(v: ClassWithOneIntIndex) = indexes
    }
  }

  case class ClassWithOneStringIndex(foo: String)
  object ClassWithOneStringIndex {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", "bars"))

    implicit def serializer = new RiakSerializer[ClassWithOneStringIndex] {
      def serialize(iv: ClassWithOneStringIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithOneStringIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneStringIndex] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithOneStringIndex] {
      def index(v: ClassWithOneStringIndex) = indexes
    }
  }

  case class ClassWithTwoIntIndexes(foo: String)
  object ClassWithTwoIntIndexes {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1), RiakIndex("bars", 42))

    implicit def serializer = new RiakSerializer[ClassWithTwoIntIndexes] {
      def serialize(iv: ClassWithTwoIntIndexes): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithTwoIntIndexes] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithTwoIntIndexes] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithTwoIntIndexes] {
      def index(v: ClassWithTwoIntIndexes) = indexes
    }
  }

  case class ClassWithTwoIntIndexesWithTheSameName(foo: String)
  object ClassWithTwoIntIndexesWithTheSameName {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1), RiakIndex("foos", 42))

    implicit def serializer = new RiakSerializer[ClassWithTwoIntIndexesWithTheSameName] {
      def serialize(iv: ClassWithTwoIntIndexesWithTheSameName): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithTwoIntIndexesWithTheSameName] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithTwoIntIndexesWithTheSameName] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithTwoIntIndexesWithTheSameName] {
      def index(v: ClassWithTwoIntIndexesWithTheSameName) = indexes
    }
  }

  case class ClassWithMixedIndexes(foo: String)
  object ClassWithMixedIndexes {
    val indexes: Set[RiakIndex] = Set(
      RiakIndex("foos", 1),
      RiakIndex("foos", 2),
      RiakIndex("foos", "foos"),
      RiakIndex("foos", "bars"),
      RiakIndex("bars", 42),
      RiakIndex("foobars", "bacon")
    )


    implicit def serializer = new RiakSerializer[ClassWithMixedIndexes] {
      def serialize(iv: ClassWithMixedIndexes): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithMixedIndexes] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithMixedIndexes] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithMixedIndexes] {
      def index(v: ClassWithMixedIndexes) = indexes
    }
  }

  case class ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces(foo: String)
  object ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", "index1, index2"), RiakIndex("foos", "index3, index4"))

    implicit def serializer = new RiakSerializer[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces] {
      def serialize(iv: ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces] {
      def index(v: ClassWithDoubleIndexNamesAndValuesContainingCommasAndSpaces) = indexes
    }
  }
}
