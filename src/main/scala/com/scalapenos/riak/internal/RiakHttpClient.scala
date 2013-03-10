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
// RiakHttpClient
// ============================================================================

private[riak] final class RiakHttpClient(helper: RiakHttpClientHelper, server: RiakServerInfo) extends RiakClient {
  def ping = helper.ping(server)
  def bucket(name: String, resolver: ConflictResolver) = new RiakHttpBucket(helper, server, name, resolver)
}


// ============================================================================
// RiakHttpBucket
// ============================================================================

private[riak] final class RiakHttpBucket(helper: RiakHttpClientHelper, server: RiakServerInfo, bucket: String, val resolver: ConflictResolver) extends RiakBucket {
  def fetch(key: String) = helper.fetch(server, bucket, key, resolver)
  def fetch(index: RiakIndex) = helper.fetch(server, bucket, index, resolver)
  def fetch(indexRange: RiakIndexRange) = helper.fetch(server, bucket, indexRange, resolver)

  def store(key: String, value: RiakValue, returnBody: Boolean) = helper.store(server, bucket, key, value, returnBody, resolver)

  def delete(key: String) = helper.delete(server, bucket, key)

  def properties = helper.getBucketProperties(server, bucket)
  def properties_=(newProperties: Set[RiakBucketProperty[_]]) = helper.setBucketProperties(server, bucket, newProperties)
}
