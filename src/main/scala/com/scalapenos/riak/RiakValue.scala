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

import internal.DateTimeSupport._

// ============================================================================
// RiakValue
// ============================================================================

final case class RiakValue(
    data: String,
    contentType: ContentType,
    vclock: VClock,
    etag: ETag,
    lastModified: DateTime,
    indexes: Set[RiakIndex] = Set.empty[RiakIndex]) {
  def withData(newData: String): RiakValue = copy(data = newData)
  def withData(newData: String, newContentType: ContentType): RiakValue = copy(data = newData, contentType = newContentType)
  def withData[T: RiakMarshaller](data: T): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakMarshaller[T]].serialize(data)
    val indexes = implicitly[RiakMarshaller[T]].index(data)

    RiakValue(dataAsString, contentType, vclock, etag, lastModified, indexes)
  }

  def as[T: RiakDeserializer]: T = implicitly[RiakDeserializer[T]].deserialize(data, contentType)
  def asMeta[T: RiakDeserializer: RiakMarshaller]: RiakMeta[T] = RiakMeta(as[T], contentType, vclock, etag, lastModified, indexes)
}

object RiakValue {
  def apply[T: RiakMarshaller](data: T): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakMarshaller[T]].serialize(data)
    val indexes = implicitly[RiakMarshaller[T]].index(data)

    RiakValue(dataAsString, contentType, VClock.NotSpecified, ETag.NotSpecified, currentDateTimeUTC, indexes)
  }

  def apply[T: RiakMarshaller](meta: RiakMeta[T]): RiakValue = {
    val (dataAsString, contentType) = implicitly[RiakMarshaller[T]].serialize(meta.data)
    val indexes = meta.indexes ++ implicitly[RiakMarshaller[T]].index(meta.data)

    RiakValue(dataAsString, contentType, meta.vclock, meta.etag, meta.lastModified, indexes)
  }
}

// ============================================================================
// RiakMeta
// ============================================================================

final case class RiakMeta[T: RiakMarshaller](
    data: T,
    contentType: ContentType,
    vclock: VClock,
    etag: ETag,
    lastModified: DateTime,
    indexes: Set[RiakIndex] = Set.empty[RiakIndex]) {
  def map(f: T ⇒ T): RiakMeta[T] = RiakMeta(f(data), contentType, vclock, etag, lastModified)

  def toRiakValue = RiakValue(this)
}
