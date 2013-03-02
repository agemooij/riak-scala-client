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
      "n_val": 3,
      "allow_mult": true,
      "last_write_wins": false,
      "r": "quorum",
      "w": "quorum",
      "dw": "quorum",
      "rw": "quorum",

{
    "props": {
        "n_val": 3,
        "allow_mult": true,
        "last_write_wins": false,
        "name": "riak-conflict-resolution-tests",
        "dw": "quorum",
        "pr": 0,
        "pw": 0,
        "r": "quorum",
        "rw": "quorum",
        "w": "quorum",
        "notfound_ok": true,
        "basic_quorum": false,
        "precommit": [],
        "postcommit": [],
        "small_vclock": 50,
        "big_vclock": 50,
        "young_vclock": 20
        "old_vclock": 86400,
        "chash_keyfun": {
            "fun": "chash_std_keyfun",
            "mod": "riak_core_util"
        },
        "linkfun": {
            "fun": "mapreduce_linkfun",
            "mod": "riak_kv_wm_link_walker"
        },
    }
}

*/

import spray.json._


// ============================================================================
// Reading Bucket Properties
// ============================================================================

final case class RiakBucketProperties (
  numberOfReplicas: Int,
  allowSiblings: Boolean,
  lastWriteWins: Boolean //,
  // readQuorum: RiakQuorum,
  // primaryReadQuorum: RiakQuorum,
  // writeQuorum: RiakQuorum,
  // durableWriteQuorum: RiakQuorum,
  // primaryWriteQuorum: RiakQuorum,
  // deleteQuorum: RiakQuorum
)

object RiakBucketProperties {
  implicit object jsonReader extends RootJsonReader[RiakBucketProperties] {
    def read(value: JsValue): RiakBucketProperties = {
      value.asJsObject.fields.get("props").flatMap { props =>
        props.asJsObject.getFields("n_val", "allow_mult", "last_write_wins") match {
          case Seq(JsNumber(numberOfReplicas), JsBoolean(allowSiblings), JsBoolean(lastWriteWins)) =>
            Some(new RiakBucketProperties(numberOfReplicas.toInt, allowSiblings, lastWriteWins))
          case _ => None
        }
      }.getOrElse(throw new DeserializationException(s"Invalid Riak properties document: ${value.compactPrint}"))
    }
  }
}


// ============================================================================
// Writing Bucket Properties
// ============================================================================

sealed trait RiakBucketProperty[T] {
  def name: String
  def value: T
  def json: JsValue

  override def equals(other: Any): Boolean = other match {
    case that: RiakBucketProperty[_] => name == that.name
    case _                        => false
  }

  override def hashCode: Int = name.hashCode
}

final class NumberOfReplicas(val value: Int) extends RiakBucketProperty[Int] {
  def name = "n_val"
  def json = JsNumber(value)
}

object NumberOfReplicas {
  def apply(value: Int): NumberOfReplicas = new NumberOfReplicas(value)
}

final class AllowSiblings(val value: Boolean) extends RiakBucketProperty[Boolean] {
  def name = "allow_mult"
  def json = JsBoolean(value)
}

object AllowSiblings {
  def apply(value: Boolean): AllowSiblings = new AllowSiblings(value)
}

final class LastWriteWins(val value: Boolean) extends RiakBucketProperty[Boolean] {
  def name = "last_write_wins"
  def json = JsBoolean(value)
}

object LastWriteWins {
  def apply(value: Boolean): LastWriteWins = new LastWriteWins(value)
}
