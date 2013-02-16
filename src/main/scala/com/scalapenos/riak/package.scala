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

package com.scalapenos

import scala.util._
import com.github.nscala_time.time.Imports._

package object riak {

  // ============================================================================
  // Type Aliases
  // ============================================================================

  type ContentType = spray.http.ContentType
  val ContentType = spray.http.ContentType

  type DateTime = org.joda.time.DateTime
  val DateTime  = com.github.nscala_time.time.StaticDateTime


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
  // VClock/ETag Support
  // ============================================================================

  implicit class VClock(val value: String) extends AnyVal {
    def isDefined = !isEmpty
    def isEmpty = value.trim.isEmpty
    def toOption: Option[VClock] = if (isDefined) Some(this) else None

    override def toString = value
  }

  object VClock {
    val NotSpecified = new VClock("")

    implicit def vclockToString(vclock: VClock): String = vclock.toString
  }


  implicit class ETag(val value: String) extends AnyVal {
    def isDefined = !isEmpty
    def isEmpty = value.trim.isEmpty
    def toOption: Option[ETag] = if (isDefined) Some(this) else None

    override def toString = value
  }

  object ETag {
    val NotSpecified = ETag("")

    implicit def etagToString(etag: ETag): String = etag.toString
  }


  // ============================================================================
  // Exceptions
  // ============================================================================

  case class BucketOperationFailed(cause: String) extends RuntimeException(cause)
  case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
  case class ParametersInvalid(cause: String) extends RuntimeException(cause)

}
