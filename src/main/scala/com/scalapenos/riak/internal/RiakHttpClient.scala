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

import scala.concurrent.Future


private[riak] final class RiakHttpClient(helper: RiakHttpClientHelper, server: RiakServerInfo) extends RiakClient {
  def ping = helper.ping(server)
  def bucket(name: String, resolver: RiakConflictsResolver) = new RiakHttpBucket(helper, server, name, resolver)
  def createSearchIndex(name: String , schema:String = "_yz_default")  = helper.createSearchIndex(server, name, schema)
  def getSearchIndex(name: String) = helper.getSearchIndex(server, name)
}
