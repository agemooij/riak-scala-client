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

import scala.concurrent.Future
import scala.concurrent.Future._

import akka.actor._

import spray.client.HttpClient
import spray.client.pipelining._
import spray.http.{HttpEntity, HttpHeader, HttpResponse}
import spray.http.StatusCodes._
import spray.http.HttpHeaders._

import converters._
import RiakHttpHeaders._
import utils.SprayClientExtras._


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
    url.getProtocol == "https")

  def apply(host: String, port: Int, pathPrefix: String = "", useSSL: Boolean = false): RiakServerInfo = new RiakServerInfo(host, port, pathPrefix.dropWhile(_ == '/'), useSSL)
}


// ============================================================================
// RiakHttpClient
//
// TODO: add Retry support, maybe at the bucket level
// TODO: use URL-escaping to make sure all keys and bucket names are valid
// ============================================================================

private[riak] class RiakHttpClient(system: ActorSystem) {
  import system.dispatcher

  private val httpClient = system.actorOf(Props(new HttpClient()), "riak-http-client")
  private val settings = RiakClientExtension(system).settings


  def fetch(server: RiakServerInfo, bucket: String, key: String, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(url(server, bucket, key))).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response))
        case NotFound        => successful(None)
        case MultipleChoices => resolveConflict(server, bucket, key, response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def store(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, returnBody: Boolean, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    // TODO: add the Last-Modified value from the RiakValue as a header
    // TODO: add the eTag value from the RiakValue as a header if it is non-empty

    // TODO: add any indexes defined on the RiakValue as index headers

    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader(`X-Riak-Vclock`, vclock.toString))
    val request = addOptionalHeader(vclockHeader) ~> httpRequest

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

  import java.net.URLEncoder

  private def encode(in: String) = URLEncoder.encode(in, "UTF-8")

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


  // ==========================================================================
  // Response => RiakValue
  // ==========================================================================

  private def toRiakValue(response: HttpResponse): Option[RiakValue] = toRiakValue(response.entity, response.headers)
  private def toRiakValue(entity: HttpEntity, headers: List[HttpHeader]): Option[RiakValue] = {
    import com.github.nscala_time.time.Imports._

    entity.toOption.flatMap { body =>
      val vClockOption       = headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).map(_.value)
      val eTagOption         = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified"))
                                      .map(h => new DateTime(h.asInstanceOf[`Last-Modified`].date.clicks))

      // TODO: make sure the DateTime is always in the Zulu zone

      for (vClock <- vClockOption; eTag <- eTagOption; lastModified <- lastModifiedOption)
      yield RiakValue(body.buffer, body.contentType, vClock, eTag, lastModified)
    }
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
