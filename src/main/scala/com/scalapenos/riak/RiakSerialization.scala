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

import annotation.implicitNotFound
import scala.util.control.NoStackTrace


// ============================================================================
// Serialization
// ============================================================================

/**
 * A RiakSerializer is a type class trait for implementing serialization from some
 * type T to a Tuple2 of raw data (a String) and a ContentType.
 */
@implicitNotFound(msg = "Cannot find RiakSerializer type class for ${T}")
trait RiakSerializer[T] {
  /** Serializes an instance of T to a Tuple2 of raw data (a String) and a ContentType. */
  def serialize(t: T): (String, ContentType)
}

/**
 * Contains lowest-priority default implementations of the RiakSerializer type class.
 */
object RiakSerializer extends LowPriorityDefaultRiakSerializerImplicits

private[riak] trait LowPriorityDefaultRiakSerializerImplicits {
  implicit def stringSerializer = new RiakSerializer[String] {
    def serialize(s: String): (String, ContentType) = (s, ContentTypes.`text/plain`)
  }

  import serialization.SprayJsonSerialization.SprayJsonSerializer
  import spray.json.RootJsonWriter
  implicit def sprayJsonSerializer[T: RootJsonWriter] = new SprayJsonSerializer[T]
}


// ============================================================================
// Deserialization
// ============================================================================

/**
 * A RiakDeserializer is a type class trait for implementing deserialization from some
 * raw data (a String) and a ContentType to a type T.
 */
@implicitNotFound(msg = "Cannot find RiakDeserializer type class for ${T}")
trait RiakDeserializer[T] {
  /**
   * Deserializes from some raw data and a ContentType to a type T.
   * @throws RiakDeserializationException if the content could not be converted to an instance of T.
   */
  def deserialize(data: String, contentType: ContentType): T
}

/**
 * Contains lowest-priority default implementations of the RiakDeserializer type class.
 */
object RiakDeserializer extends LowPriorityDefaultRiakDeserializerImplicits

private[riak] trait LowPriorityDefaultRiakDeserializerImplicits {
  implicit def stringDeserializer = new RiakDeserializer[String] {
    def deserialize(data: String, contentType: ContentType): String = data
  }

  import serialization.SprayJsonSerialization.SprayJsonDeserializer
  import spray.json.RootJsonReader
  import scala.reflect.ClassTag
  implicit def sprayJsonDeserializer[T: RootJsonReader: ClassTag] = new SprayJsonDeserializer[T]
}

/**
 * Base exception used to denote problems while deserializing from raw Strings to some type T.
 * @see RiakDeserializer
 */
sealed abstract class RiakDeserializationException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

/**
 * Exception used to denote a lower-level exception occurred while deserializing from raw Strings to some type T.
 * @see RiakDeserializer
 */
case class RiakDeserializationFailed(data: String, targetType: String, cause: Throwable)
  extends RiakDeserializationException(s"Unable to deserialize data to target type '$targetType'. Reason: '${cause.getMessage}'. Raw data: '$data'.", cause)

/**
 * Exception used to denote that deserializing failed because the raw data was of an unsupported ContentType.
 * @see RiakDeserializer
 */
case class RiakUnsupportedContentType(expected: ContentType, actual: ContentType)
  extends RiakDeserializationException(s"Unexpected ContentType during deserialization: expected '$expected' but got '$actual'.")
  with NoStackTrace
