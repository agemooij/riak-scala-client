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
import akka.actor._
import com.github.nscala_time.time.Imports._

import converters._


// ============================================================================
// The Main API definitions
// ============================================================================



// ============================================================================
// RiakClient - The main entry point
// ============================================================================

case class RiakClient(system: ActorSystem) {
  def connect(host: String, port: Int) = RiakExtension(system).connect(host, port)
  def connect(): RiakConnection = connect("localhost", 8098)

  def apply(host: String, port: Int): RiakConnection = connect(host, port)
}

object RiakClient {
  def apply(): RiakClient = apply(ActorSystem("riak-client"))
}


// ============================================================================
// RiakExtension - The root of the actor tree
// ============================================================================

object RiakExtension extends ExtensionId[RiakExtension] with ExtensionIdProvider {
  def lookup() = RiakExtension
  def createExtension(system: ExtendedActorSystem) = new RiakExtension(system)
}

class RiakExtension(system: ExtendedActorSystem) extends Extension {
  // TODO: how to deal with:
  //       - Shutting down the ActorSystem when we're done and we created the actor system to begin with)
  //       - someone else shutting down the ActorSystem, leaving us in an invalid state
  // TODO: add system shutdown hook

  // TODO: create new connection actor and wrap it in a RiakConnectionImpl

  // TODO: implement and expose a Settings class
  // val settings = new RiakSettings(system.settings.config)

  val httpClient = RiakHttpClient(system: ActorSystem)

  def connect(host: String, port: Int) = new RiakConnectionImpl(httpClient, host, port)
}


// ============================================================================
// RiakConnection
// ============================================================================

trait RiakConnection {
  import resolvers.LastValueWinsResolver

  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): Bucket
}

private[riak] case class RiakConnectionImpl(httpClient: RiakHttpClient, host: String, port: Int) extends RiakConnection {
  def bucket(name: String, resolver: ConflictResolver) = BucketImpl(httpClient, host, port, name, resolver)
}


// ============================================================================
// Bucket
// ============================================================================

trait Bucket {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: use URL-escaping to make sure all keys (and bucket names) are valid

  def resolver: ConflictResolver

  def fetch(key: String): Future[Option[RiakValue]]

  def store(key: String, value: RiakValue): Future[Option[RiakValue]]

  def store[T: RiakValueWriter](key: String, value: T): Future[Option[RiakValue]] = {
    store(key, implicitly[RiakValueWriter[T]].write(value))
  }

  // TODO: add support for storing without a key, putting the generated key into the RiakValue which it should then always produce.
  // def store(value: RiakValue): Future[String]
  // def store[T: RiakValueWriter](value: T): Future[String]

  def delete(key: String): Future[Unit]
}

private[riak] case class BucketImpl(httpClient: RiakHttpClient, host: String, port: Int, bucket: String, resolver: ConflictResolver) extends Bucket {
  def fetch(key: String) = httpClient.fetch(host, port, bucket, key, resolver)

  def store(key: String, value: RiakValue) = httpClient.store(host, port, bucket, key, value, resolver)

  def delete(key: String) = httpClient.delete(host, port, bucket, key)
}


// ============================================================================
// RiakHttpClient
// ============================================================================

import spray.httpx.RequestBuilding

private[riak] case class RiakHttpClient(system: ActorSystem) extends RequestBuilding {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: use URL-escaping to make sure all keys (and bucket names) are valid

  import system.dispatcher
  import spray.client.HttpClient
  import spray.client.pipelining._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.http.HttpHeaders._
  import utils.SprayClientExtras._

  private[this] val httpClient = system.actorOf(Props(new HttpClient), "riak-http-client")

  def fetch(host: String, port: Int, bucket: String, key: String, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(url(host, port, bucket, key))).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case MultipleChoices => resolveConflict(host, port, bucket, key, response, resolver)
        case NotFound        => None
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def store(host: String, port: Int, bucket: String, key: String, value: RiakValue, resolver: ConflictResolver): Future[Option[RiakValue]] = {
    // TODO: Add a nice, non-intrusive way to set query parameters, like 'returnbody', etc.

    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader("X-Riak-Vclock", vclock.toString))
    val request = addOptionalHeader(vclockHeader) ~> httpRequest

    request(Put(url(host, port, bucket, key) + "?returnbody=true", value)).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case NoContent       => None
        case MultipleChoices => resolveConflict(host, port, bucket, key, response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed(s"Store for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
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


  private val clientId = "%s".format(java.util.UUID.randomUUID())

  private def httpRequest = {
    // TODO: make the client id optional based on some config (Settings in reference.conf)

    addHeader("X-Riak-ClientId", clientId) ~> sendReceive(httpClient)
  }

  private def url(host: String, port: Int, bucket: String, key: String) = s"http://$host:$port/buckets/$bucket/keys/$key"

  private def toRiakValue(response: HttpResponse): Option[RiakValue] = toRiakValue(response.entity, response.headers)
  private def toRiakValue(entity: HttpEntity, headers: List[HttpHeader]): Option[RiakValue] = {
    entity.toOption.flatMap { body =>
      val vClockOption       = headers.find(_.is("x-riak-vclock")).map(_.value)
      val eTagOption         = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified"))
                                      .map(h => new DateTime(h.asInstanceOf[`Last-Modified`].date.clicks))

      // TODO: make sure the DateTime is always in the Zulu zone

      for (vClock <- vClockOption; eTag <- eTagOption; lastModified <- lastModifiedOption)
      yield RiakValue(body.buffer, body.contentType, vClock, eTag, lastModified)
    }
  }

  private def resolveConflict(host: String, port: Int, bucket: String, key: String, response: HttpResponse, resolver: ConflictResolver): Option[RiakValue] = {
    import spray.http._
    import spray.httpx.unmarshalling._

    response.entity.as[MultipartContent] match {
      case Left(error) => throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) => {
        val values = multipartContent.parts.flatMap(part => toRiakValue(part.entity, part.headers)).toSet
        val value = resolver.resolve(values)

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // TODO: the resolved value needs to be stored back so the resolution sticks !!!!
        //        we then need to get that value back and get the vclock from there  !!!!
        // TODO: make sure that the body gets returned
        // hm, how am I going to flatten this future??
        //store(host, port, bucket, key, value)

        Some(value)
      }
    }
  }
}
