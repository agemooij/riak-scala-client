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


private[riak] trait RiakUrlSupport {

  // ==========================================================================
  // URL building and Query Parameters
  // ==========================================================================

  def urlEncode(in: String) = java.net.URLEncoder.encode(in, "UTF-8")
  def urlDecode(in: String) = java.net.URLDecoder.decode(in, "UTF-8")

  sealed trait QueryParameters {
    def queryString: String
  }

  case object NoQueryParameters extends QueryParameters {
    def queryString = ""
  }

  case class StoreQueryParameters(returnBody: Boolean = false) extends QueryParameters {
    def queryString = s"?returnbody=$returnBody"
  }

  def baseUrl(server: RiakServerInfo): String = {
    val protocol = if (server.useSSL) "https" else "http"
    val pathPrefix = if (server.pathPrefix.isEmpty) "" else s"${server.pathPrefix}/"

    s"$protocol://${server.host}:${server.port}/${pathPrefix}"
  }

  def pingUrl(server: RiakServerInfo): String = {
    s"${baseUrl(server)}ping"
  }

  def bucketUrl(server: RiakServerInfo, bucket: String): String = {
    s"${baseUrl(server)}buckets/${urlEncode(bucket)}"
  }

  def url(server: RiakServerInfo, bucket: String, key: String, parameters: QueryParameters = NoQueryParameters): String = {
    s"${bucketUrl(server, bucket)}/keys/${urlEncode(key)}${parameters.queryString}"
  }

  def bucketPropertiesUrl(server: RiakServerInfo, bucket: String): String = {
    s"${bucketUrl(server, bucket)}/props"
  }

  def mapReduceUrl(server: RiakServerInfo): String = {
    s"${baseUrl(server)}mapred"
  }

  def indexUrl(server: RiakServerInfo, bucket: String, index: RiakIndex): String = {
    // both index name and String index values are double-urlEncoded because Riak eagerly urlDecodes the request
    // and then tries to match the urlDecoded value against our urlEncoded indexes
    val indexName = urlEncode(urlEncode(index.fullName))
    val indexValue = index.value match {
      case l: Long => l.toString
      case s: String => urlEncode(urlEncode(s))
    }

    s"${bucketUrl(server, bucket)}/index/${indexName}/$indexValue"
  }

  def indexRangeUrl(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange): String = {
    // both index name and String index values are double-urlEncoded because Riak eagerly urlDecodes the request
    // and then tries to match the urlDecoded value against our urlEncoded indexes
    val indexName = urlEncode(urlEncode(indexRange.fullName))
    val (indexStart, indexEnd) = indexRange.start match {
      case l: Long => (indexRange.start.toString, indexRange.end.toString)
      case s: String => (urlEncode(urlEncode(indexRange.start.toString)), urlEncode(urlEncode(indexRange.end.toString)))
    }

    s"${bucketUrl(server, bucket)}/index/${indexName}/${indexStart}/${indexEnd}"
  }
}
