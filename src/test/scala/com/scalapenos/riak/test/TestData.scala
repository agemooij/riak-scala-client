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
package test


object TestClassesWithIndexes {
  import scala.util._

  case class ClassWithOneIntIndex(foo: String)
  object ClassWithOneIntIndex {
    implicit def serializer = new RiakSerializer[ClassWithOneIntIndex] {
      def serialize(iv: ClassWithOneIntIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithOneIntIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneIntIndex] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithOneIntIndex] {
      def index(v: ClassWithOneIntIndex) = Set(RiakIndex("foos", 1))
    }
  }

  case class ClassWithOneStringIndex(foo: String)
  object ClassWithOneStringIndex {
    implicit def serializer = new RiakSerializer[ClassWithOneStringIndex] {
      def serialize(iv: ClassWithOneStringIndex): (String, ContentType) = (iv.foo, ContentType.`text/plain`)
    }

    implicit def deserializer = new RiakDeserializer[ClassWithOneStringIndex] {
      def deserialize(data: String, contentType: ContentType): Try[ClassWithOneStringIndex] = Success(apply(data))
    }

    implicit def indexer = new RiakIndexer[ClassWithOneStringIndex] {
      def index(v: ClassWithOneStringIndex) = Set(RiakIndex("foos", v.foo))
    }
  }
}
