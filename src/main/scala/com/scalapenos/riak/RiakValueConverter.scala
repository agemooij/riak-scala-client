/*
 * Copyright (C) 2011-2012 scalapenos.com
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
  def write(obj: T): RiakValue = write(obj, VClock.NotSpecified)
  def write(obj: T, vclock: VClock): RiakValue
}

/**
  * Provides the RiakValue deserialization and serialization for type T.
 */
trait RiakValueConverter[T] extends RiakValueReader[T] with RiakValueWriter[T]


case class ConversionFailedException(message: String, cause: Throwable = null) extends RuntimeException(message, cause) with NoStackTrace
