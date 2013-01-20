/*
 * Copyright (C) 2011-2012 scalapenos.com
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
// RiakHttpClient
//
// TODO: add Retry support, maybe at the bucket level
// TODO: use URL-escaping to make sure all keys (and bucket names) are valid
// ============================================================================

private[riak] case class RiakHttpClient(system: ActorSystem) {
  import system.dispatcher

  private[this] val httpClient = system.actorOf(Props(new HttpClient), "riak-http-client")


  def fetch(host: String, port: Int, bucket: String, key: String, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(url(host, port, bucket, key))).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response))
        case NotFound        => successful(None)
        case MultipleChoices => resolveConflict(host, port, bucket, key, response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def store(host: String, port: Int, bucket: String, key: String, value: RiakValue, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    // TODO: Add a nice, non-intrusive way to set query parameters, like 'returnbody', etc.

    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader(`X-Riak-Vclock`, vclock.toString))
    val request = addOptionalHeader(vclockHeader) ~> httpRequest

    request(Put(url(host, port, bucket, key) + "?returnbody=true", value)).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response))
        case NoContent       => successful(None)
        case MultipleChoices => resolveConflict(host, port, bucket, key, response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Store for of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def delete(host: String, port: Int, bucket: String, key: String): Future[Unit] = {
    httpRequest(Delete(url(host, port, bucket, key))).map { response =>
      response.status match {
        case NoContent       => ()
        case NotFound        => ()
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Delete for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }


  private lazy val clientId = java.util.UUID.randomUUID().toString

  private def httpRequest = {
    // TODO: make the client id optional based on some config (Settings in reference.conf)

    addHeader(`X-Riak-ClientId`, clientId) ~>
    addHeader("Accept", "*/*, multipart/mixed") ~>
    sendReceive(httpClient)
  }

  private def url(host: String, port: Int, bucket: String, key: String) = s"http://$host:$port/buckets/$bucket/keys/$key"

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

  private def resolveConflict(host: String, port: Int, bucket: String, key: String, response: HttpResponse, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    import spray.http._
    import spray.httpx.unmarshalling._

    val vclockHeader = response.headers.find(_.is("x-riak-vclock")).toList

    response.entity.as[MultipartContent] match {
      case Left(error) => throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) => {
        val values = multipartContent.parts.flatMap(part => toRiakValue(part.entity, vclockHeader ++ part.headers)).toSet
        val value = resolver.resolve(values)

        // Store the resolved value back to Riak and return the resulting RiakValue
        // TODO: make sure that this one returns the body (when we make that configurable)
        store(host, port, bucket, key, value, resolver)
      }
    }
  }
}
