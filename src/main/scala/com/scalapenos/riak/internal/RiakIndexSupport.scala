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


private[riak] trait RiakIndexSupport { self: RiakUrlSupport =>
  import spray.http.HttpHeader
  import spray.http.HttpHeaders._
  import RiakHttpHeaders._

  // TODO: declare a config setting for whether we url encode the index name and/or value
  //       maybe even at the top-level (for bucket names and keys) so it matches the behaviour of the riak url compatibility setting

  private[riak] def toIndexHeader(index: RiakIndex): HttpHeader = {
    index match {
      case l: RiakLongIndex   => RawHeader(indexHeaderPrefix + urlEncode(l.fullName), l.value.toString)
      case s: RiakStringIndex => RawHeader(indexHeaderPrefix + urlEncode(s.fullName), urlEncode(s.value))
    }
  }

  private[riak] def toRiakIndexes(headers: List[HttpHeader]): Set[RiakIndex] = {
    val IndexNameAndType = (indexHeaderPrefix + "(.+)_(bin|int)$").r

    def toRiakIndex(header: HttpHeader): Set[RiakIndex] = {
      header.lowercaseName match {
        case IndexNameAndType(name, "int") => header.value.split(',').map(value => RiakIndex(urlDecode(name), value.trim.toLong)).toSet
        case IndexNameAndType(name, "bin") => header.value.split(',').map(value => RiakIndex(urlDecode(name), urlDecode(value.trim))).toSet
        case _                             => Set.empty[RiakIndex]
      }
    }

    headers.filter(_.lowercaseName.startsWith(indexHeaderPrefix))
           .flatMap(toRiakIndex(_))
           .toSet
  }

  private[riak] case class RiakIndexQueryResponse(keys: List[String])

  private[riak] object RiakIndexQueryResponse {
    import spray.json._
    import spray.json.DefaultJsonProtocol._

    implicit val format = jsonFormat1(RiakIndexQueryResponse.apply)
  }
}
