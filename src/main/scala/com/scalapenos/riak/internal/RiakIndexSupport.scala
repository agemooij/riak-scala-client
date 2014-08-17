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

import RiakHttpHeaders._

private[riak] object RiakIndexSupport {
  val IndexNameAndType = (indexHeaderPrefix + "(.+)_(bin|int)$").r
}

private[riak] trait RiakIndexSupport {
  import spray.http.HttpHeader
  import spray.http.HttpHeaders._
  import RiakIndexSupport._

  private[riak] def toIndexHeader(index: RiakIndex): HttpHeader = {
    index match {
      case l: RiakLongIndex   ⇒ RawHeader(indexHeaderPrefix + l.fullName, l.value.toString)
      case s: RiakStringIndex ⇒ RawHeader(indexHeaderPrefix + s.fullName, s.value)
    }
  }

  private[riak] def toRiakIndexes(headers: List[HttpHeader]): Set[RiakIndex] = {
    def toRiakIndex(header: HttpHeader): Set[RiakIndex] = {
      val values = header.value.split(',')

      header.lowercaseName match {
        case IndexNameAndType(name, "int") ⇒ values.map(value ⇒ RiakIndex(name, value.trim.toLong)).toSet
        case IndexNameAndType(name, "bin") ⇒ values.map(value ⇒ RiakIndex(name, value.trim)).toSet
        case _                             ⇒ Set.empty[RiakIndex]
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
