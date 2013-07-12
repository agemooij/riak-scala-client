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


sealed abstract class RiakMarshaller[T: RiakSerializer: RiakIndexer] {
  def serialize(t: T): (String, ContentType)
  def index(t: T): Set[RiakIndex]
}

object RiakMarshaller {
  implicit def default[T: RiakSerializer : RiakIndexer] = new RiakMarshaller[T] {
    def serialize(t: T): (String, ContentType) = implicitly[RiakSerializer[T]].serialize(t)
    def index(t: T): Set[RiakIndex] = implicitly[RiakIndexer[T]].index(t)
  }
}
