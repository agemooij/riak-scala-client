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


// ============================================================================
// RiakMeta
// ============================================================================

final case class RiakMeta[T: RiakSerializer: RiakIndexer](
  data: T,
  contentType: ContentType,
  vclock: VClock,
  etag: ETag,
  lastModified: DateTime,
  indexes: Set[RiakIndex] = Set.empty[RiakIndex]
) {
  def map(f: T => T): RiakMeta[T] = RiakMeta(f(data), contentType, vclock, etag, lastModified)

  def toRiakValue = RiakValue(this)
}


// ============================================================================
// RiakValue
// ============================================================================

final case class RiakValue(
  data: String,
  contentType: ContentType,
  vclock: VClock,
  etag: ETag,
  lastModified: DateTime,
  indexes: Set[RiakIndex] = Set.empty[RiakIndex]
) {
  import scala.util.Try

  def as[T: RiakDeserializer]: Try[T] = implicitly[RiakDeserializer[T]].deserialize(data, contentType)
  def toMeta[T: RiakDeserializer]: Try[RiakMeta[T]] = as[T].map(data => RiakMeta(data, contentType, vclock, etag, lastModified, indexes))
}

object RiakValue {
  def apply[T: RiakSerializer: RiakIndexer](data: T): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakSerializer[T]].serialize(data)
    val indexes = implicitly[RiakIndexer[T]].index(data)

    RiakValue(dataAsString, contentType, VClock.NotSpecified, ETag.NotSpecified, DateTime.now, indexes)
  }

  def apply[T: RiakSerializer: RiakIndexer](meta: RiakMeta[T]): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakSerializer[T]].serialize(meta.data)
    val indexes = meta.indexes ++ implicitly[RiakIndexer[T]].index(meta.data)

    RiakValue(dataAsString, contentType, meta.vclock, meta.etag, DateTime.now, indexes)
  }
}
