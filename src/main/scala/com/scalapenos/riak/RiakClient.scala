/*
 * Copyright (C) 2011-2012 scalapenos.com
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

import scala.concurrent.Future
import akka.actor._

import converters._


// ============================================================================
// RiakClient - The main entry point
// ============================================================================

case class RiakClient(system: ActorSystem) {
  def connect(host: String, port: Int) = RiakExtension(system).connect(host, port)
  def connect(): RiakConnection = connect("localhost", 8098)

  def apply(host: String, port: Int): RiakConnection = connect(host, port)
}

object RiakClient {
  lazy val system = ActorSystem("riak-client")

  def apply(): RiakClient = apply(system)
}


// ============================================================================
// RiakExtension - The root of the actor tree
// ============================================================================

object RiakExtension extends ExtensionId[RiakExtension] with ExtensionIdProvider {
  def lookup() = RiakExtension
  def createExtension(system: ExtendedActorSystem) = new RiakExtension(system)
}

class RiakExtension(system: ExtendedActorSystem) extends Extension {
  // TODO: how to deal with:
  //       - Shutting down the ActorSystem when we're done and we created the actor system to begin with)
  //       - someone else shutting down the ActorSystem, leaving us in an invalid state
  // TODO: add system shutdown hook

  // TODO: implement and expose a Settings class
  // val settings = new RiakSettings(system.settings.config)

  val httpClient = RiakHttpClient(system: ActorSystem)

  def connect(host: String, port: Int) = new RiakConnectionImpl(httpClient, host, port)
}


// ============================================================================
// RiakConnection
// ============================================================================

trait RiakConnection {
  import resolvers.LastValueWinsResolver

  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): Bucket
}

private[riak] case class RiakConnectionImpl(httpClient: RiakHttpClient, host: String, port: Int) extends RiakConnection {
  def bucket(name: String, resolver: ConflictResolver) = BucketImpl(httpClient, host, port, name, resolver)
}


// ============================================================================
// Bucket
// ============================================================================

trait Bucket {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: use URL-escaping to make sure all keys (and bucket names) are valid

  def resolver: ConflictResolver

  def fetch(key: String): Future[Option[RiakValue]]

  def store(key: String, value: RiakValue): Future[Option[RiakValue]]

  def store[T: RiakValueWriter](key: String, value: T): Future[Option[RiakValue]] = {
    store(key, implicitly[RiakValueWriter[T]].write(value))
  }

  // TODO: add support for storing without a key, putting the generated key into the RiakValue which it should then always produce.
  // def store(value: RiakValue): Future[String]
  // def store[T: RiakValueWriter](value: T): Future[String]

  def delete(key: String): Future[Unit]

  // TODO: implement support for reading and writing bucket properties
  // def properties: Future[BucketProperties]
  // def properties_=(props: BucketProperties): Future[Unit]
}

private[riak] case class BucketImpl(httpClient: RiakHttpClient, host: String, port: Int, bucket: String, resolver: ConflictResolver) extends Bucket {
  def fetch(key: String) = httpClient.fetch(host, port, bucket, key, resolver)

  def store(key: String, value: RiakValue) = httpClient.store(host, port, bucket, key, value, resolver)

  def delete(key: String) = httpClient.delete(host, port, bucket, key)
}
