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

/*

TODO:

Add indexes to RiakValue
Allows converters to define their own indexes

How to detect whether indexes are available (i.e. whether the Riak backend is leveldb)?

Make converters stackable/delegatable so you could for instance use the standard spray json converter AND add extra indexes
*/


sealed trait RiakIndex {
  type Type
  def name: String
  def value: Type
}

object RiakIndex {
  def apply(name: String, value: String) = RiakStringIndex(name, value)
  def apply(name: String, value: Int) = RiakIntIndex(name, value)
}

final case class RiakStringIndex(name: String, value: String) extends RiakIndex {
  type Type = String
}

final case class RiakIntIndex(name: String, value: Int) extends RiakIndex {
  type Type = Int
}


import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find RiakIndex type class for ${T}")
trait RiakIndexer[T] {
  def index(t: T): Set[RiakIndex]
}

object RiakIndexer extends LowPriorityDefaultRiakIndexerImplicits

trait LowPriorityDefaultRiakIndexerImplicits {
  implicit def defaultNoIndexes[T] = new RiakIndexer[T] {
    def index(t: T) = Set.empty[RiakIndex]
  }
}
