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
  import scala.concurrent.{ ExecutionContext, Future }
  import internal._
  import RiakBucket._

  val name: String

  /**
   * Every bucket has a default RiakConflictsResolver that will be used when resolving
   * conflicts during fetches and stores (when returnBody is true).
   */
  def resolver: RiakConflictsResolver

  def fetch(key: String, conditionalParams: ConditionalRequestParam*): Future[Option[RiakValue]]
  def fetchWithSiblings(key: String): Future[Option[Set[RiakValue]]]

  def fetch(index: String, value: String): Future[List[RiakValue]] = fetch(RiakIndex(index, value))
  def fetch(index: String, value: Int): Future[List[RiakValue]] = fetch(RiakIndex(index, value))
  def fetch(index: RiakIndex): Future[List[RiakValue]]

  def fetch(index: String, start: String, end: String): Future[List[RiakValue]] = fetch(RiakIndexRange(index, start, end))
  def fetch(index: String, start: Int, end: Int): Future[List[RiakValue]] = fetch(RiakIndexRange(index, start, end))
  private[riak] def fetch(index: RiakIndexRange): Future[List[RiakValue]]

  def store[T: RiakMarshaller](key: String, value: T): Future[Unit] = store(key, RiakValue(value))
  def store[T: RiakMarshaller](key: String, meta: RiakMeta[T]): Future[Unit] = store(key, RiakValue(meta))
  def store(key: String, value: RiakValue): Future[Unit]

  def storeAndFetch[T: RiakMarshaller](key: String, value: T): Future[RiakValue] = storeAndFetch(key, RiakValue(value))
  def storeAndFetch[T: RiakMarshaller](key: String, meta: RiakMeta[T]): Future[RiakValue] = storeAndFetch(key, RiakValue(meta))
  def storeAndFetch(key: String, value: RiakValue): Future[RiakValue]

  def delete(key: String): Future[Unit]

  def properties: Future[RiakBucketProperties]
  def properties_=(newProperties: Set[RiakBucketProperty[_]]): Future[Unit]
  def setProperties(newProperties: Set[RiakBucketProperty[_]]): Future[Unit] = properties_=(newProperties)

  def n_val(implicit ec: ExecutionContext): Future[Int] = numberOfReplicas
  def numberOfReplicas(implicit ec: ExecutionContext): Future[Int] = properties.map(_.numberOfReplicas)
  def n_val_=(number: Int): Future[Unit] = setNumberOfReplicas(number)
  def numberOfReplicas_=(number: Int): Future[Unit] = setNumberOfReplicas(number)
  def setNumberOfReplicas(number: Int): Future[Unit] = setProperties(Set(NumberOfReplicas(number)))

  def allow_mult(implicit ec: ExecutionContext): Future[Boolean] = allowSiblings
  def allowSiblings(implicit ec: ExecutionContext): Future[Boolean] = properties.map(_.allowSiblings)
  def allow_mult_=(value: Boolean): Future[Unit] = setAllowSiblings(value)
  def allowSiblings_=(value: Boolean): Future[Unit] = setAllowSiblings(value)
  def setAllowSiblings(value: Boolean): Future[Unit] = setProperties(Set(AllowSiblings(value)))

  def last_write_wins(implicit ec: ExecutionContext): Future[Boolean] = lastWriteWins
  def lastWriteWins(implicit ec: ExecutionContext): Future[Boolean] = properties.map(_.lastWriteWins)
  def last_write_wins_=(value: Boolean): Future[Unit] = setLastWriteWins(value)
  def lastWriteWins_=(value: Boolean): Future[Unit] = setLastWriteWins(value)
  def setLastWriteWins(value: Boolean): Future[Unit] = setProperties(Set(LastWriteWins(value)))

  def unsafe: UnsafeBucketOperations
}

object RiakBucket {

  /**
   * Parameter for conditional request semantics.
   * Can be used for Fetch Value and Store Value operations.
   */
  sealed trait ConditionalRequestParam

  /**
   * Perform a request on a RiakValue only if value's ETag does not match the given one.
   *
   * @param eTag the target ETag value.
   */
  case class IfNotMatch(eTag: ETag) extends ConditionalRequestParam

  /**
   * Perform a request on a RiakValue only if value's ETag matches the given one.
   *
   * @param eTag the target ETag value.
   */
  case class IfMatch(eTag: ETag) extends ConditionalRequestParam

  /**
   * Perform a request on a RiakValue only if value's Last-Modified time is after the given timestamp.
   *
   * @param timestamp
   *
   * *Note*: if target time is in the future then Riak always treats this condition as `true`.
   */
  case class IfModifiedSince(timestamp: DateTime) extends ConditionalRequestParam

  /**
   * Perform a request on a RiakValue only if value's Last-Modified time is before the given timestamp.
   *
   * @param timestamp
   *
   * *Note*: if target time is in the future then Riak always treats this condition as `true`.
   */
  case class IfUnmodifiedSince(timestamp: DateTime) extends ConditionalRequestParam
}
