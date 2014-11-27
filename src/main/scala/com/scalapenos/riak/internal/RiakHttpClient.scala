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

  //Bucket
  def bucket(name: String,
             riakBucketType: RiakBucketType = bucketType(name="default"),
             resolver: RiakConflictsResolver = DefaultConflictsResolver) =
    new RiakHttpBucket(helper, server, name, resolver, riakBucketType)

  def getBuckets(riakBucketType: RiakBucketType = bucketType(name="default")) =
    helper.getBuckets(server, this, riakBucketType.name)

  //Bucket Type
  def bucketType(name: String) = new RiakHttpBucketType(helper, server, this, name)

  //Map Reduce
  def mapReduce(input: RiakMapReduce.Input) = new RiakHttpMapReduce(helper, server, input)

  //Client methods
  //Search Index
  def createSearchIndex(name: String , nVal:Int = 3, schema:String = "_yz_default")  =
    helper.createSearchIndex(server, name, nVal, schema)
  def getSearchIndex(name: String) = helper.getSearchIndex(server, name)
  def getSearchIndexList = helper.getSearchIndexList(server)
  def deleteSearchIndex(name: String) = helper.deleteSearchIndex(server, name)

  //Search Schema
  def createSearchSchema(name:String, content:scala.xml.Elem):Future[Boolean] =
    helper.createSearchSchema(server, name, content)
  def getSearchSchema(name:String):Future[scala.xml.Elem] = helper.getSearchSchema(server, name)

  def search(index:RiakSearchIndex, query:RiakSearchQuery):Future[RiakSearchResult] = helper.search(server, index, query)

  //def getBuckets():Future[RiakBucket] = helper.search(server, index, query)
}