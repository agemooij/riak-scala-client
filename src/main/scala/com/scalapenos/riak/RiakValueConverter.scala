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
  * Provides the RiakValue deserialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakValueReader or RiakValueConverter type class for ${T}")
trait RiakValueReader[T] {
  def read(value: RiakValue): Try[T]
}

/**
  * Provides the RiakValue serialization for type T.
 */
@implicitNotFound(msg = "Cannot find RiakValueWriter or RiakValueConverter type class for ${T}")
trait RiakValueWriter[T] {
  def contentType: ContentType
  def serialize(t: T): String
  // def indexes(t: T): Set[RiakIndex]

  final def write(t: T): RiakValue = RiakValue(serialize(t), contentType, VClock.NotSpecified, ETag.NotSpecified, DateTime.now)
  final def write(meta: RiakMeta[T]): RiakValue = RiakValue(serialize(meta.data), contentType, meta.vclock, meta.etag, DateTime.now)
}

/**
  * Provides the RiakValue deserialization and serialization for type T.
 */
trait RiakValueConverter[T] extends RiakValueReader[T] with RiakValueWriter[T]


case class ConversionFailedException(message: String, cause: Throwable = null) extends RuntimeException(message, cause) with NoStackTrace
