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


trait RiakBucket {
  import scala.concurrent.Future
  import internal._

  /**
   * Every bucket has a default ConflictResolver that will be used when resolving
   * conflicts during fetches and stores (when returnBody is true).
   */
  def resolver: ConflictResolver

  /**
   *
   */
  def fetch(key: String): Future[Option[RiakValue]]

  /**
   *
   */
  def fetch(index: String, value: String): Future[List[RiakValue]] = fetch(RiakIndex(index, value))
  def fetch(index: String, value: Int): Future[List[RiakValue]] = fetch(RiakIndex(index, value))
  def fetch(index: RiakIndex): Future[List[RiakValue]]

  def fetch(index: String, start: String, end: String): Future[List[RiakValue]] = fetch(RiakIndexRange(index, start, end))
  def fetch(index: String, start: Int, end: Int): Future[List[RiakValue]] = fetch(RiakIndexRange(index, start, end))
  private[riak] def fetch(index: RiakIndexRange): Future[List[RiakValue]]

  /**
   *
   */
  def store[T: RiakSerializer: RiakIndexer](key: String, value: T): Future[Option[RiakValue]] = {
    store(key, value, false)
  }

  /**
   *
   */
  def store[T: RiakSerializer: RiakIndexer](key: String, value: T, returnBody: Boolean): Future[Option[RiakValue]] = {
    store(key, RiakValue(value), returnBody)
  }

  /**
   *
   */
  def store[T: RiakSerializer: RiakIndexer](key: String, meta: RiakMeta[T]): Future[Option[RiakValue]] = {
    store(key, meta, false)
  }

  /**
   *
   */
  def store[T: RiakSerializer: RiakIndexer](key: String, meta: RiakMeta[T], returnBody: Boolean): Future[Option[RiakValue]] = {
    store(key, RiakValue(meta), returnBody)
  }

  /**
   *
   */
  def store(key: String, value: RiakValue): Future[Option[RiakValue]] = {
    store(key, value, false)
  }

  /**
   *
   */
  def store(key: String, value: RiakValue, returnBody: Boolean): Future[Option[RiakValue]]


  /**
   *
   */
  def delete(key: String): Future[Unit]


  // TODO: implement support for reading and writing bucket properties
  // def allow_mult: Future[Boolean]
  // def allow_mult_=(allow: Boolean): Future[Unit]


  /* Writable bucket properties:

  {
    "props": {
      "n_val": 3,
      "allow_mult": true,
      "last_write_wins": false,
      "precommit": [],
      "postcommit": [],
      "r": "quorum",
      "w": "quorum",
      "dw": "quorum",
      "rw": "quorum",
      "backend": ""
    }
  }

  */
}

