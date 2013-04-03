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

import akka.actor._

// ============================================================================
// RiakClient - The main entry point
// ============================================================================

object RiakClient {
  private val defaultHost = "localhost"
  private val defaultPort = 8098
  private lazy val internalSystem = ActorSystem("riak-client")

  def apply()                                             : RiakClient = RiakClientExtension(internalSystem).connect(defaultHost, defaultPort)
  def apply(host: String, port: Int)                      : RiakClient = RiakClientExtension(internalSystem).connect(host, port)
  def apply(url: String)                                  : RiakClient = RiakClientExtension(internalSystem).connect(url)
  def apply(url: java.net.URL)                            : RiakClient = RiakClientExtension(internalSystem).connect(url)
  def apply(system: ActorSystem)                          : RiakClient = RiakClientExtension(system).connect(defaultHost, defaultPort)
  def apply(system: ActorSystem, host: String, port: Int) : RiakClient = RiakClientExtension(system).connect(host, port)
  def apply(system: ActorSystem, url: String)             : RiakClient = RiakClientExtension(system).connect(url)
  def apply(system: ActorSystem, url: java.net.URL)       : RiakClient = RiakClientExtension(system).connect(url)
}

trait RiakClient {
  import scala.concurrent.Future

  // TODO: stats

  def ping: Future[Boolean]
  def bucket(name: String, resolver: RiakConflictsResolver = DefaultConflictsResolver): RiakBucket
  def mapReduce(input: RiakMapReduce.Input): RiakMapReduce
}


// ============================================================================
// RiakClientExtension - The root of the actor tree
// ============================================================================

object RiakClientExtension extends ExtensionId[RiakClientExtension] with ExtensionIdProvider {
  def lookup() = RiakClientExtension
  def createExtension(system: ExtendedActorSystem) = new RiakClientExtension(system)
}

class RiakClientExtension(system: ExtendedActorSystem) extends Extension {
  import internal._

  private[riak] val settings = new RiakClientSettings(system.settings.config)
  private[riak] lazy val httpHelper = new RiakHttpClientHelper(system: ActorSystem)

  def connect(url: String): RiakClient = connect(RiakServerInfo(url))
  def connect(url: java.net.URL): RiakClient = connect(RiakServerInfo(url))
  def connect(host: String, port: Int): RiakClient = connect(RiakServerInfo(host, port))

  private def connect(server: RiakServerInfo): RiakClient = new RiakHttpClient(httpHelper, server)
}
