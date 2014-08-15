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

package com.scalapenos.riak.internal

import com.scalapenos.riak.{RiakBucketProperty, RiakBucketType}

private[riak] final class RiakHttpBucketType(helper: RiakHttpClientHelper, server: RiakServerInfo, val name: String) extends RiakBucketType {

  def getProperties = helper.getBucketTypeProperties(server, name)
  def setProperties(newProperties: Set[RiakBucketProperty[_]]) = helper.setBucketTypeProperties(server, name, newProperties)
}
