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

import akka.actor._


// ============================================================================
// RiakClient - The main entry point
// ============================================================================

case class RiakClient(system: ActorSystem) {
  def connect(): RiakConnection = connect("localhost", 8098)
  def connect(host: String, port: Int): RiakConnection = RiakClientExtension(system).connect(host, port)
  def connect(url: String): RiakConnection = RiakClientExtension(system).connect(url)
  def connect(url: java.net.URL): RiakConnection = RiakClientExtension(system).connect(url)

  def apply(host: String, port: Int): RiakConnection = connect(host, port)
  def apply(url: String): RiakConnection = connect(url)
  def apply(url: java.net.URL): RiakConnection = connect(url)
}

object RiakClient {
  lazy val system = ActorSystem("riak-client")

  def apply(): RiakClient = apply(system)
}


// ============================================================================
// RiakClientExtension - The root of the actor tree
// ============================================================================

object RiakClientExtension extends ExtensionId[RiakClientExtension] with ExtensionIdProvider {
  def lookup() = RiakClientExtension
  def createExtension(system: ExtendedActorSystem) = new RiakClientExtension(system)
}

class RiakClientExtension(system: ExtendedActorSystem) extends Extension {
  private[riak] val settings = new RiakClientSettings(system.settings.config)
  private[riak] lazy val httpClient = new RiakHttpClient(system: ActorSystem)

  def connect(url: String): RiakConnection = connect(RiakServerInfo(url))
  def connect(url: java.net.URL): RiakConnection = connect(RiakServerInfo(url))
  def connect(host: String, port: Int): RiakConnection = connect(RiakServerInfo(host, port))

  private def connect(server: RiakServerInfo): RiakConnection = new HttpConnection(httpClient, server)
}


// ============================================================================
// RiakConnection
// ============================================================================

trait RiakConnection {
  import resolvers.LastValueWinsResolver

  // TODO: ping and stats

  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): RiakBucket
}


// ============================================================================
// Bucket
// ============================================================================

trait RiakBucket {
  import scala.concurrent.Future

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

  // def fetch(index: String, lowerBound: String, upperBound: String): Future[List[RiakValue]]
  // def fetch(index: String, lowerBound: Int, upperBound: Int): Future[List[RiakValue]]

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



// ============================================================================
// Private Implementations
// ============================================================================

private[riak] class HttpConnection(httpClient: RiakHttpClient, server: RiakServerInfo) extends RiakConnection {
  def bucket(name: String, resolver: ConflictResolver) = new HttpBucket(httpClient, server, name, resolver)
}

private[riak] class HttpBucket(httpClient: RiakHttpClient, server: RiakServerInfo, bucket: String, val resolver: ConflictResolver) extends RiakBucket {
  def fetch(key: String) = httpClient.fetch(server, bucket, key, resolver)
  def fetch(index: RiakIndex) = httpClient.fetch(server, bucket, index, resolver)

  def store(key: String, value: RiakValue, returnBody: Boolean) = httpClient.store(server, bucket, key, value, returnBody, resolver)

  def delete(key: String) = httpClient.delete(server, bucket, key)
}
