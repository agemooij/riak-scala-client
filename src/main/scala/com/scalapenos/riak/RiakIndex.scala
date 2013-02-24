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

/*
  TODO: How to detect whether indexes are available (i.e. whether the Riak backend is leveldb)?
*/

// ============================================================================
//  RiakIndex
// ============================================================================

sealed trait RiakIndex {
  type Type
  def name: String
  def suffix: String
  def value: Type

  def fullName = s"${name}_${suffix}"
}

object RiakIndex {
  def apply(name: String, value: String) = RiakStringIndex(name, value)
  def apply(name: String, value: Long) = RiakLongIndex(name, value)
}

final case class RiakStringIndex(name: String, value: String) extends RiakIndex {
  type Type = String
  def suffix = "bin"
}

final case class RiakLongIndex(name: String, value: Long) extends RiakIndex {
  type Type = Long
  def suffix = "int"
}


// ============================================================================
//  RiakIndexer
// ============================================================================

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find RiakIndex type class for ${T}")
trait RiakIndexer[T] {
  def index(t: T): Set[RiakIndex]
}

object RiakIndexer extends LowPriorityDefaultRiakIndexerImplicits

private[riak] trait LowPriorityDefaultRiakIndexerImplicits {
  implicit def defaultNoIndexes[T] = new RiakIndexer[T] {
    def index(t: T) = Set.empty[RiakIndex]
  }
}
