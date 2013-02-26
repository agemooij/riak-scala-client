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
package internal


// ============================================================================
// RiakIndexRange - for easy passing of a fetch range
// ============================================================================

private[riak] sealed trait RiakIndexRange {
  type Type
  def name: String
  def suffix: String
  def start: Type
  def end: Type

  def fullName = s"${name}_${suffix}"
}

private[riak] object RiakIndexRange {
  def apply(name: String, start: String, end: String) = RiakStringIndexRange(name, start, end)
  def apply(name: String, start: Long, end: Long) = RiakLongIndexRange(name, start, end)
}

private[riak] final case class RiakStringIndexRange(name: String, start: String, end: String) extends RiakIndexRange {
  type Type = String
  def suffix = "bin"
}

private[riak] final case class RiakLongIndexRange(name: String, start: Long, end: Long) extends RiakIndexRange {
  type Type = Long
  def suffix = "int"
}
