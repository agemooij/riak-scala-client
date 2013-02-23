/*
 * Copyright (C) 2012-2013 Age Mooij
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


// ============================================================================
// RiakServerInfo - a nice way to encode the Riak server properties
// ============================================================================

private[riak] class RiakServerInfo(val host: String, val port: Int, val pathPrefix: String = "", val useSSL: Boolean = false)

private[riak] object RiakServerInfo {
  import java.net.URL

  def apply(url: String): RiakServerInfo = apply(new URL(url))
  def apply(url: URL): RiakServerInfo = apply(
    url.getHost,
    if (url.getPort != -1) url.getPort else url.getDefaultPort,
    url.getPath,
    url.getProtocol.toLowerCase == "https")

  def apply(host: String, port: Int, pathPrefix: String = "", useSSL: Boolean = false): RiakServerInfo = new RiakServerInfo(host, port, pathPrefix.dropWhile(_ == '/'), useSSL)
}


// ============================================================================
// RiakIndexRange - for easy passing of a fetch range
// ============================================================================

private[riak] sealed trait RiakIndexRange {
  type Type
  def name: String
  def suffix: String
  def start: Type
  def end: Type

  def fullName = s"${name}_${suffix}"
}

private[riak] object RiakIndexRange {
  def apply(name: String, start: String, end: String) = RiakStringIndexRange(name, start, end)
  def apply(name: String, start: Long, end: Long) = RiakLongIndexRange(name, start, end)
}

private[riak] final case class RiakStringIndexRange(name: String, start: String, end: String) extends RiakIndexRange {
  type Type = String
  def suffix = "bin"
}

private[riak] final case class RiakLongIndexRange(name: String, start: Long, end: Long) extends RiakIndexRange {
  type Type = Long
  def suffix = "int"
}


// ============================================================================
// RiakHttpClient
//
// TODO: add Retry support, maybe at the bucket level
// ============================================================================

import akka.actor._

private[riak] object RiakHttpClient {
  import spray.http.HttpBody
  import spray.httpx.marshalling._

  /**
   * Spray Marshaller for turning RiakValue instances into HttpEntity instances so they can be sent to Riak.
   */
  implicit val RiakValueMarshaller: Marshaller[RiakValue] = new Marshaller[RiakValue] {
    def apply(riakValue: RiakValue, ctx: MarshallingContext) {
      ctx.marshalTo(HttpBody(riakValue.contentType, riakValue.data.getBytes(riakValue.contentType.charset.nioCharset)))
    }
  }
}

private[riak] class RiakHttpClient(system: ActorSystem) {
  import scala.concurrent.Future
  import scala.concurrent.Future._

  import spray.client.HttpClient
  import spray.client.pipelining._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.http.HttpHeaders._

  import org.slf4j.LoggerFactory

  import utils.SprayClientExtras._
  import RiakHttpHeaders._
  import RiakHttpClient._

  import system.dispatcher

  private val httpClient = system.actorOf(Props(new HttpClient()), "riak-http-client")
  private val settings = RiakClientExtension(system).settings
  private val log = LoggerFactory.getLogger(getClass)


  // ==========================================================================
  // Main HTTP Request Implementations
  // ==========================================================================

  def fetch(server: RiakServerInfo, bucket: String, key: String, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(url(server, bucket, key))).flatMap { response =>
      response.status match {
        case OK                 => successful(toRiakValue(response))
        case NotFound           => successful(None)
        case MultipleChoices    => resolveConflict(server, bucket, key, response, resolver)
        case BadRequest         => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other              => throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case NotModified => successful(None)
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, index: RiakIndex, resolver: ConflictResolver): Future[List[RiakValue]] = {
    httpRequest(Get(url(server, bucket, index))).flatMap { response =>
      response.status match {
        case OK              => fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest      => throw new ParametersInvalid(s"""Invalid index name ("${index.fullName}") or value ("${index.value}").""")
        case other           => throw new BucketOperationFailed(s"""Fetch for index "${index.fullName}" with value "${index.value}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange, resolver: ConflictResolver): Future[List[RiakValue]] = {
    httpRequest(Get(url(server, bucket, indexRange))).flatMap { response =>
      response.status match {
        case OK              => fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest      => throw new ParametersInvalid(s"""Invalid index name ("${indexRange.fullName}") or range ("${indexRange.start}" to "${indexRange.start}").""")
        case other           => throw new BucketOperationFailed(s"""Fetch for index "${indexRange.fullName}" with range "${indexRange.start}" to "${indexRange.start}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def store(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, returnBody: Boolean, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    // TODO: add the Last-Modified value from the RiakValue as a header

    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader(`X-Riak-Vclock`, vclock))
    val etagHeader = value.etag.toOption.map(etag => RawHeader(`ETag`, etag))
    val indexHeaders = value.indexes.map(toIndexHeader(_)).toList

    val request = addOptionalHeader(vclockHeader) ~>
                  addOptionalHeader(etagHeader) ~>
                  addHeaders(indexHeaders) ~>
                  httpRequest

    request(Put(url(server, bucket, key, StoreQueryParameters(returnBody)), value)).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response))
        case NoContent       => successful(None)
        case MultipleChoices => resolveConflict(server, bucket, key, response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Store for of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def delete(server: RiakServerInfo, bucket: String, key: String): Future[Unit] = {
    httpRequest(Delete(url(server, bucket, key))).map { response =>
      response.status match {
        case NoContent       => ()
        case NotFound        => ()
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Delete for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }


  // ==========================================================================
  // Request building
  // ==========================================================================

  private lazy val clientId = java.util.UUID.randomUUID().toString
  private val clientIdHeader = if (settings.AddClientIdHeader) Some(RawHeader(`X-Riak-ClientId`, clientId)) else None

  private def httpRequest = {
    addOptionalHeader(clientIdHeader) ~>
    addHeader("Accept", "*/*, multipart/mixed") ~>
    sendReceive(httpClient)
  }


  // ==========================================================================
  // URL building and Query Parameters
  // ==========================================================================

  private def encode(in: String) = java.net.URLEncoder.encode(in, "UTF-8")
  private def decode(in: String) = java.net.URLDecoder.decode(in, "UTF-8")

  private sealed trait QueryParameters {
    def queryString: String
  }

  private case object NoQueryParameters extends QueryParameters {
    def queryString = ""
  }

  private case class StoreQueryParameters(returnBody: Boolean = false) extends QueryParameters {
    def queryString = s"?returnbody=$returnBody"
  }

  private def url(server: RiakServerInfo, bucket: String, key: String, parameters: QueryParameters = NoQueryParameters): String = {
    val protocol = if (server.useSSL) "https" else "http"
    val pathPrefix = if (server.pathPrefix.isEmpty) "" else s"${server.pathPrefix}/"

    s"$protocol://${server.host}:${server.port}/${pathPrefix}buckets/${encode(bucket)}/keys/${encode(key)}${parameters.queryString}"
  }

  private def url(server: RiakServerInfo, bucket: String, index: RiakIndex): String = {
    val protocol = if (server.useSSL) "https" else "http"
    val pathPrefix = if (server.pathPrefix.isEmpty) "" else s"${server.pathPrefix}/"

    // both index name and index value are double-encoded because Riak eagerly decodes the request
    // and then tries to match the decoded value against our encoded indexes
    val indexName = encode(encode(index.fullName))
    val indexValue = index.value match {
      case l: Long => l.toString
      case s: String => encode(encode(s))
    }

    s"$protocol://${server.host}:${server.port}/${pathPrefix}buckets/${encode(bucket)}/index/${indexName}/$indexValue"
  }

  private def url(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange): String = {
    val protocol = if (server.useSSL) "https" else "http"
    val pathPrefix = if (server.pathPrefix.isEmpty) "" else s"${server.pathPrefix}/"

    // both index name and index value are double-encoded because Riak eagerly decodes the request
    // and then tries to match the decoded value against our encoded indexes
    val indexName = encode(encode(indexRange.fullName))
    val (indexStart, indexEnd) = indexRange.start match {
      case l: Long => (indexRange.start.toString, indexRange.end.toString)
      case s: String => (encode(encode(indexRange.start.toString)), encode(encode(indexRange.end.toString)))
    }

    s"$protocol://${server.host}:${server.port}/${pathPrefix}buckets/${encode(bucket)}/index/${indexName}/${indexStart}/${indexEnd}"
  }


  // ==========================================================================
  // Response => RiakValue
  // ==========================================================================

  private def toRiakValue(response: HttpResponse): Option[RiakValue] = toRiakValue(response.entity, response.headers)
  private def toRiakValue(entity: HttpEntity, headers: List[HttpHeader]): Option[RiakValue] = {
    entity.toOption.flatMap { body =>
      val vClockOption       = headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).map(_.value)
      val eTagOption         = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified"))
                                      .map(h => new DateTime(h.asInstanceOf[`Last-Modified`].date.clicks))
      val indexes            = toRiakIndexes(headers)

      // TODO: make sure the DateTime is always in the Zulu zone

      for (vClock <- vClockOption; eTag <- eTagOption; lastModified <- lastModifiedOption)
      yield RiakValue(body.asString, body.contentType, vClock, eTag, lastModified, indexes)
    }
  }

  // ==========================================================================
  // HttpHeader <=> RiakIndex
  // ==========================================================================

  // TODO: declare a config setting for whether we encode the index name and/or value
  //       maybe even at the top-level (for bucket names and keys) so it matches the behaviour of the riak url compatibility setting

  private def toIndexHeader(index: RiakIndex): HttpHeader = {
    index match {
      case l: RiakLongIndex   => RawHeader(indexHeaderPrefix + encode(l.fullName), l.value.toString)
      case s: RiakStringIndex => RawHeader(indexHeaderPrefix + encode(s.fullName), encode(s.value))
    }
  }

  private def toRiakIndexes(headers: List[HttpHeader]): Set[RiakIndex] = {
    val IndexNameAndType = (indexHeaderPrefix + "(.+)_(bin|int)$").r

    def toRiakIndex(header: HttpHeader): Set[RiakIndex] = {
      header.lowercaseName match {
        case IndexNameAndType(name, "int") => header.value.split(',').map(value => RiakIndex(decode(name), value.trim.toLong)).toSet
        case IndexNameAndType(name, "bin") => header.value.split(',').map(value => RiakIndex(decode(name), decode(value.trim))).toSet
        case _                             => Set.empty[RiakIndex]
      }
    }

    headers.filter(_.lowercaseName.startsWith(indexHeaderPrefix))
           .flatMap(toRiakIndex(_))
           .toSet
  }

  case class RiakIndexQueryResponse(keys: List[String])
  object RiakIndexQueryResponse {
    import spray.json._
    import spray.json.DefaultJsonProtocol._

    implicit val format = jsonFormat1(RiakIndexQueryResponse.apply)
  }

  private def fetchWithKeysReturnedByIndexLookup(server: RiakServerInfo, bucket: String, response: HttpResponse, resolver: ConflictResolver): Future[List[RiakValue]] = {
    response.entity.toOption.map { body =>
      import spray.json._

      val keys = body.asString.asJson.convertTo[RiakIndexQueryResponse].keys

      traverse(keys)(fetch(server, bucket, _, resolver)).map(_.flatten)
    }.getOrElse(successful(Nil))
  }


  // ==========================================================================
  // Conflict Resolution
  // ==========================================================================

  private def resolveConflict(server: RiakServerInfo, bucket: String, key: String, response: HttpResponse, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    import spray.http._
    import spray.httpx.unmarshalling._

    val vclockHeader = response.headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).toList

    response.entity.as[MultipartContent] match {
      case Left(error) => throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) => {
        val values = multipartContent.parts.flatMap(part => toRiakValue(part.entity, vclockHeader ++ part.headers)).toSet
        val value = resolver.resolve(values)

        // Store the resolved value back to Riak and return the resulting RiakValue
        store(server, bucket, key, value, true, resolver)
      }
    }
  }
}
