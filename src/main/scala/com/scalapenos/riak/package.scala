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

package com.scalapenos

import spray.http.HttpMethods._
import spray.http.{HttpMethods, HttpMethod}
import spray.httpx.RequestBuilding

/**
 * A fast, non-blocking Scala client library for interacting with Riak.
 *
 *
 * This package also defines some type aliases provided as shortcuts to commonly
 * used classes from other libraries. For example, ContentType is an alias for
 * [[spray.http.ContentType]] and DateTime is an alias for [[org.joda.time.DateTime]].
 *
 * @version 0.8.0
 *
 */
package object riak {

  // ============================================================================
  // Type Aliases
  // ============================================================================

  type ContentType = spray.http.ContentType
  val ContentType = spray.http.ContentType
  val ContentTypes = spray.http.ContentTypes

  type DateTime = org.joda.time.DateTime

  val MediaTypes = spray.http.MediaTypes

  // ============================================================================
  // Conflict Resolution
  // ============================================================================

  case class ConflictResolution(result: RiakValue, writeBack: Boolean)

  trait RiakConflictsResolver  {
    def resolve(values: Set[RiakValue]): ConflictResolution
  }

  implicit def func2resolver(f: Set[RiakValue] => ConflictResolution): RiakConflictsResolver = new RiakConflictsResolver {
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

  case class OperationFailed(cause: String) extends RuntimeException(cause)
  case class BucketOperationFailed(cause: String) extends RuntimeException(cause)
  case class BucketTypeOperationFailed(cause: String) extends RuntimeException(cause)
  case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
  case class ParametersInvalid(cause: String) extends RuntimeException(cause)
  case class MapReduceOperationFailed(cause: String) extends RuntimeException(cause)

}
