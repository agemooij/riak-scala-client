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


// ============================================================================
// Serialization
// ============================================================================

/**
  * Provides the RiakValue serialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakSerializer type class for ${T}")
trait RiakSerializer[T] {
  def serialize(t: T): (String, ContentType)
}

object RiakSerializer extends LowPriorityDefaultRiakSerializerImplicits

trait LowPriorityDefaultRiakSerializerImplicits {
  implicit def toStringSerializer[T] = new RiakSerializer[T] {
    def serialize(t: T): (String, ContentType) = (t.toString, ContentType.`text/plain`)
  }

  implicit def stringSerializer = new RiakSerializer[String] {
    def serialize(s: String): (String, ContentType) = (s, ContentType.`text/plain`)
  }
}


// ============================================================================
// Deserialization
// ============================================================================

/**
  * Provides the RiakValue deserialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakDeserializer type class for ${T}")
trait RiakDeserializer[T] {
  def deserialize(data: String, contentType: ContentType): Try[T]
}

object RiakDeserializer extends LowPriorityDefaultRiakDeserializerImplicits

trait LowPriorityDefaultRiakDeserializerImplicits {
  import scala.util._

  implicit def stringDeserializer = new RiakDeserializer[String] {
    def deserialize(data: String, contentType: ContentType): Try[String] = Success(data)
  }
}

sealed trait RiakDeserializationException

case class RiakDeserializationFailedException(data: String, targetType: String, cause: Throwable)
  extends RuntimeException(s"Unable to deserialize data to target type '$targetType'. Reason: '${cause.getMessage}'. Data: '$data'.", cause)
  with RiakDeserializationException

case class RiakUnsupportedContentTypeException(expected: ContentType, actual: ContentType)
  extends RuntimeException(s"Unexpected ContentType during deserialization: expected '$expected' but got '$actual'.")
  with RiakDeserializationException
  with NoStackTrace
