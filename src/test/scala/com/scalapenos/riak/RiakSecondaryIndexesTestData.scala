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


object RiakSecondaryIndexesTestData {
  import scala.util._

  case class ClassWithOneIntIndex(foo: String)
  object ClassWithOneIntIndex {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1))

    implicit def serializer = plainTextSerializer[ClassWithOneIntIndex](_.foo)
    implicit def deserializer = planTextDeserializer(ClassWithOneIntIndex.apply)
    implicit def indexer = simpleIndexer[ClassWithOneIntIndex](indexes)
  }

  case class ClassWithOneStringIndex(foo: String)
  object ClassWithOneStringIndex {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", "bars"))

    implicit def serializer = plainTextSerializer[ClassWithOneStringIndex](_.foo)
    implicit def deserializer = planTextDeserializer(ClassWithOneStringIndex.apply)
    implicit def indexer = simpleIndexer[ClassWithOneStringIndex](indexes)
  }

  case class ClassWithTwoIntIndexes(foo: String)
  object ClassWithTwoIntIndexes {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1), RiakIndex("bars", 42))

    implicit def serializer = plainTextSerializer[ClassWithTwoIntIndexes](_.foo)
    implicit def deserializer = planTextDeserializer(ClassWithTwoIntIndexes.apply)
    implicit def indexer = simpleIndexer[ClassWithTwoIntIndexes](indexes)
  }

  case class ClassWithTwoIntIndexesWithTheSameName(foo: String)
  object ClassWithTwoIntIndexesWithTheSameName {
    val indexes: Set[RiakIndex] = Set(RiakIndex("foos", 1), RiakIndex("foos", 42))

    implicit def serializer = plainTextSerializer[ClassWithTwoIntIndexesWithTheSameName](_.foo)
    implicit def deserializer = planTextDeserializer(ClassWithTwoIntIndexesWithTheSameName.apply)
    implicit def indexer = simpleIndexer[ClassWithTwoIntIndexesWithTheSameName](indexes)
  }

  case class ClassWithMixedIndexes(foo: String)
  object ClassWithMixedIndexes {
    val indexes: Set[RiakIndex] = Set(
      RiakIndex("foos", 1),
      RiakIndex("foos", 2),
      RiakIndex("foos", "bars"),
      RiakIndex("foos", "barsbars"),
      RiakIndex("bars", 42)
      // these will not be supported until we find a better way to deal with encoding/decoding them in urls and headers
      // RiakIndex("foos", "foos foos"),
      // RiakIndex("foos", "bars, bars"),
      // RiakIndex("foo bars", "bacon")
    )

    implicit def serializer = plainTextSerializer[ClassWithMixedIndexes](_.foo)
    implicit def deserializer = planTextDeserializer[ClassWithMixedIndexes](apply)
    implicit def indexer = simpleIndexer[ClassWithMixedIndexes](indexes)
  }

  case class ClassWithConfigurableIntIndex(foo: String, indexedBy: Int)
  object ClassWithConfigurableIntIndex {
    val indexName = "foos"
    implicit def serializer = plainTextSerializer[ClassWithConfigurableIntIndex](_.foo)
    implicit def deserializer = planTextDeserializer[ClassWithConfigurableIntIndex](data => ClassWithConfigurableIntIndex(data, -1))
    implicit def indexer = new RiakIndexer[ClassWithConfigurableIntIndex] {
      def index(t: ClassWithConfigurableIntIndex) = Set(RiakIndex(indexName, t.indexedBy))
    }
  }

  case class ClassWithConfigurableStringIndex(foo: String, indexedBy: String)
  object ClassWithConfigurableStringIndex {
    val indexName = "bars"
    implicit def serializer = plainTextSerializer[ClassWithConfigurableStringIndex](_.foo)
    implicit def deserializer = planTextDeserializer[ClassWithConfigurableStringIndex](data => ClassWithConfigurableStringIndex(data, ""))
    implicit def indexer = new RiakIndexer[ClassWithConfigurableStringIndex] {
      def index(t: ClassWithConfigurableStringIndex) = Set(RiakIndex(indexName, t.indexedBy))
    }
  }


  def plainTextSerializer[T](ser: T => String) = new RiakSerializer[T] {
    def serialize(t: T): (String, ContentType) = (ser(t), ContentTypes.`text/plain`)
  }

  def planTextDeserializer[T](d: String => T) = new RiakDeserializer[T] {
    def deserialize(data: String, contentType: ContentType): T = d(data)
  }

  def simpleIndexer[T](indexes: Set[RiakIndex]) = new RiakIndexer[T] {
    def index(t: T) = indexes
  }
}
