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

import annotation.implicitNotFound
import scala.util._
import scala.util.control.NoStackTrace


/**
  * Provides the RiakValue serialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakSerializer type class for ${T}")
trait RiakSerializer[T] {
  def serialize(t: T): (String, ContentType)
}

/**
  * Provides the RiakValue deserialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakDeserializer type class for ${T}")
trait RiakDeserializer[T] {
  def deserialize(data: String, contentType: ContentType): Try[T]
}


// ============================================================================
// Utils for working with (De)Serializers
// ============================================================================

case class UnsupportedContentTypeException(expected: ContentType, actual: ContentType)
  extends RuntimeException(s"Unexpected ContentType during deserialization: expected $expected but got $actual.") with NoStackTrace

object RiakSerializerSupport {
  def toRiakValue[T: RiakSerializer](data: T): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakSerializer[T]].serialize(data)

    RiakValue(dataAsString, contentType, VClock.NotSpecified, ETag.NotSpecified, DateTime.now)
  }

  def toRiakValue[T: RiakSerializer](meta: RiakMeta[T]): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakSerializer[T]].serialize(meta.data)

    RiakValue(dataAsString, contentType, meta.vclock, meta.etag, DateTime.now)
  }
}

object RiakDeserializerSupport {
  def deserialize[T: RiakDeserializer](value: RiakValue): Try[T] = {
    implicitly[RiakDeserializer[T]].deserialize(value.data, value.contentType)
  }
}


// ============================================================================
// Lowest priority implicit (de)serializers to/from String
// ============================================================================

object DefaultRiakSerializationSupport extends DefaultRiakSerializationSupport

trait DefaultRiakSerializationSupport extends DefaultRiakSerializationLowPriorityImplicits

trait DefaultRiakSerializationLowPriorityImplicits {
  import scala.util._

  implicit def toStringSerializer[T] = new RiakSerializer[T] {
    def serialize(t: T): (String, ContentType) = (t.toString, ContentType.`text/plain`)
  }

  implicit def stringSerializer = new RiakSerializer[String] {
    def serialize(s: String): (String, ContentType) = (s, ContentType.`text/plain`)
  }

  implicit def toStringDeserializer = new RiakDeserializer[String] {
    def deserialize(data: String, contentType: ContentType): Try[String] = Success(data)
  }
}
