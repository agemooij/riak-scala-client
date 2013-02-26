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
import internal._


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

  private def connect(server: RiakServerInfo): RiakConnection = new RiakHttpConnection(httpClient, server)
}
