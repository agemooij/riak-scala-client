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
// RiakServerInfo - a nice way to encode the Riak server properties
// ============================================================================

private[riak] class RiakServerInfo(val host: String, val port: Int, val pathPrefix: String = "", val useSSL: Boolean = false) {
  val protocol = if (useSSL) "https" else "http"
}

private[riak] object RiakServerInfo {
  import java.net.URL

  def apply(url: String): RiakServerInfo = apply(new URL(url))
  def apply(url: URL): RiakServerInfo = apply(
    url.getHost,
    if (url.getPort != -1) url.getPort else url.getDefaultPort,
    url.getPath,
    url.getProtocol.toLowerCase == "https")

  def apply(host: String, port: Int, pathPrefix: String = "", useSSL: Boolean = false): RiakServerInfo =
    new RiakServerInfo(host, port, pathPrefix.dropWhile(_ == '/'), useSSL)
}
