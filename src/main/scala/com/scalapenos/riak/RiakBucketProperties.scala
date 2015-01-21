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

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}


// ============================================================================
// Reading Bucket Properties
// ============================================================================

final case class RiakBucketProperties (
  nVal: Int,
  allowMult: Boolean,
  lastWriteWins: Boolean,
  preCommit: List[Map[String, String]],
  search:Option[Boolean],
  searchIndex:Option[String]
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
        Some(RiakBucketProperties(
          nVal = props.asJsObject.fields.get("n_val").map(_.convertTo[Int]).get,
          allowMult = props.asJsObject.fields.get("allow_mult").map(_.convertTo[Boolean]).get,
          lastWriteWins = props.asJsObject.fields.get("last_write_wins").map(_.convertTo[Boolean]).get,
          preCommit = props.asJsObject.fields.get("precommit").map(_.convertTo[List[Map[String, String]]]).get,
          search = props.asJsObject.fields.get("search").map(_.convertTo[Boolean]),
          searchIndex = props.asJsObject.fields.get("search_index").map(_.convertTo[String])
        ))
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

case class NumberOfReplicas(value: Int) extends RiakBucketProperty[Int] {
  require(value > 0, s"Number of replicas (n_val) must be an integer larger than 0. You specified $value.")

  def name = "n_val"
  def json = JsNumber(value)
}

case class AllowMult(value: Boolean) extends RiakBucketProperty[Boolean] {
  def name = "allow_mult"
  def json = JsBoolean(value)
}

case class LastWriteWins(value: Boolean) extends RiakBucketProperty[Boolean] {
  def name = "last_write_wins"
  def json = JsBoolean(value)
}

case class PreCommit(value:List[Map[String, String]]) extends RiakBucketProperty[List[Map[String, String]]] {
  def name = "precommit"
  def json = JsArray(value.map{case x:Map[String, String] => x.toJson})
}

case class SearchIndex(value:String) extends RiakBucketProperty[String] {
  def name = "search_index"
  def json = JsString(value)
}



trait RiakBucketBasicProperties {

  val name: String

  def getProperties: Future[RiakBucketProperties]
  def setProperties(newProperties: Set[RiakBucketProperty[_]]): Future[Unit]

  def nVal(implicit ec: ExecutionContext): Future[Int] = getProperties.map(_.nVal)
  def nVal_=(number: Int): Future[Unit] = setProperties(Set(NumberOfReplicas(number)))

  def allowMult(implicit ec: ExecutionContext): Future[Boolean] = getProperties.map(_.allowMult)
  def allowMult_=(value: Boolean): Future[Unit] = setProperties(Set(AllowMult(value)))

  def lastWriteWins(implicit ec: ExecutionContext): Future[Boolean] = getProperties.map(_.lastWriteWins)
  def lastWriteWins_=(value: Boolean): Future[Unit] = setProperties(Set(LastWriteWins(value)))

  def preCommit(implicit ec: ExecutionContext): Future[List[Map[String, Any]]] = getProperties.map(_.preCommit)
  def preCommit_=(value: List[Map[String, String]]): Future[Unit] = setProperties(Set(PreCommit(value)))

  def getSearchIndex(implicit ec: ExecutionContext): Future[Option[String]] = getProperties.map(_.searchIndex)
}

/*
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
