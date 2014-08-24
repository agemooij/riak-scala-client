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

import scala.collection.LinearSeq


private[riak] trait RiakUriSupport {
  import spray.http.Uri
  import spray.http.Uri._

  // ==========================================================================
  // Query Parameters
  // ==========================================================================

  sealed trait QueryParameters {
    def query: Query
  }

  case object NoQueryParameters extends QueryParameters {
    def query = Query.Empty
  }

  case class StoreQueryParameters(returnBody: Boolean = false) extends QueryParameters {
    def query = ("returnbody", s"$returnBody") +: Query.Empty
  }

  case class SolrQueryParameters(params:Map[String, String]) extends QueryParameters {
    def query:Query = Query(params)
  }


  // ==========================================================================
  // URL building and Query Parameters
  // ==========================================================================

  def PingUri(server: RiakServerInfo) =
    uri(server, "ping")

  def KeyUri(server: RiakServerInfo, bucket: String, key: String, parameters: QueryParameters = NoQueryParameters) =
    uri(server, s"buckets/${bucket}/keys/${key}", parameters.query)

  def KeyBucketTypeUri(server: RiakServerInfo, bucket: String, bucketType:String, key: String, parameters: QueryParameters = NoQueryParameters) =
    uri(server, s"types/${bucketType}/buckets/${bucket}/keys/${key}", parameters.query)

  def BucketPropertiesUri(server: RiakServerInfo, bucket: String) =
    uri(server, s"buckets/${bucket}/props")

  def BucketWithBucketTypePropertiesUri(server: RiakServerInfo, bucket:String, bucketType:String) =
    uri(server, s"types/${bucketType}/buckets/${bucket}/props")

  def BucketTypePropertiesUri(server: RiakServerInfo,  bucketType:String) =
    uri(server, s"types/${bucketType}/props")

  def IndexUri(server: RiakServerInfo, bucket: String, index: RiakIndex) =
    uri(server, s"buckets/${bucket}/index/${index.fullName}/${index.value}")

  def IndexBucketTypeUri(server: RiakServerInfo, bucket: String, bucketType: String, index: RiakIndex) =
    uri(server, s"types/${bucketType}/buckets/${bucket}/index/${index.fullName}/${index.value}")

  def IndexRangeUri(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange) =
    uri(server, s"buckets/${bucket}/index/${indexRange.fullName}/${indexRange.start}/${indexRange.end}")

  def IndexRangeBucketTypeUri(server: RiakServerInfo, bucket: String, bucketType: String, indexRange: RiakIndexRange) =
    uri(server, s"types/${bucketType}/buckets/${bucket}/index/${indexRange.fullName}/${indexRange.start}/${indexRange.end}")

  def SearchSolrUri(server: RiakServerInfo, indexName: String, parameters: QueryParameters) =
    uri(server, s"search/query/${indexName}", parameters.query)

  def SearchIndexUri(server: RiakServerInfo, name: String) =
    uri(server, s"search/index/${name}")

  def ListSearchIndexUri(server: RiakServerInfo) =
    uri(server, s"search/index/")

  private def uri(server: RiakServerInfo, path: String, query: Query = Query.Empty): Uri = {
    Uri.from(
      scheme = server.protocol,
      host = server.host,
      port = server.port,
      path = if (server.pathPrefix.isEmpty) s"/$path" else s"/${server.pathPrefix}/$path",
      query = query
    )
  }
}
