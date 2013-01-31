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

package com.scalapenos

import scala.util._


package object riak {

  // ============================================================================
  // Conflict Resolution
  // ============================================================================

  trait ConflictResolver {
    def resolve(values: Set[RiakValue]): RiakValue
  }

  implicit def func2resolver(f: Set[RiakValue] => RiakValue): ConflictResolver = new ConflictResolver {
    def resolve(values: Set[RiakValue]) = f(values)
  }


  // ============================================================================
  // VClocks Support
  // ============================================================================

  implicit class VClock(val value: String) extends AnyVal {
    def isDefined = !isEmpty
    def isEmpty = value.isEmpty
    def toOption: Option[VClock] = if (isDefined) Some(this) else None
    override def toString = value
  }

  object VClock {
    val NotSpecified = new VClock("")
  }

  case class VClocked[T](value: T, vclock: VClock) {
    def map(newValue: T) = copy(value = newValue)
    def map[X](f: T => X) = VClocked(f(value), vclock)

    def toRiakValue(implicit writer: RiakValueWriter[T]) = implicitly[RiakValueWriter[T]].write(value, vclock)
  }

  object VClocked {
    def apply[T: RiakValueReader](riakValue: RiakValue): Try[VClocked[T]] = riakValue.as[T].map(VClocked(_, riakValue.vclock))
  }


  // ============================================================================
  // Exceptions
  // ============================================================================

  case class BucketOperationFailed(cause: String) extends RuntimeException(cause)
  case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
  case class ParametersInvalid(cause: String) extends RuntimeException(cause)

}
