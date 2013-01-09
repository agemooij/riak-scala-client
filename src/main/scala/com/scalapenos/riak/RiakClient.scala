package com.scalapenos.riak

import scala.concurrent.Future
import akka.actor._
import com.github.nscala_time.time.Imports._

import converters._

// ============================================================================
// Riak (the main entry point)
// ============================================================================

trait Riak {
  import resolvers.LastValueWinsResolver

  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): Bucket
}

case class RiakClient(system: ActorSystem, host: String, port: Int) extends Riak {
  import spray.can.client.HttpClient
  import spray.client._
  import spray.io.IOExtension

  // TODO: migrate to the new spray-client architecture (as of snapshot version 20130108)

  // TODO: make the connection manage one single RiakConnectionActor, with its
  //       own supervisor strategy for better fault tolerance. Getting a handle
  //       on the bucket could be implemented using a reference to a child actor
  //       wrapped in another trait

  private[this] val ioBridge = IOExtension(system).ioBridge()
  private[this] val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))
  private[this] val httpConduit = system.actorOf(Props(new HttpConduit(
    httpClient = httpClient,
    host = host,
    port = port,
    dispatchStrategy = DispatchStrategies.Pipelined // TODO: read this from the settings
  )))

  def bucket(name: String, resolver: ConflictResolver) = BucketImpl(system, httpConduit, name, resolver)
}

object RiakClient {
  def apply(): RiakClient = apply(ActorSystem("riak-client"))
  def apply(host: String, port: Int): RiakClient = RiakClient(ActorSystem("riak-client"), host, port)
  def apply(system: ActorSystem): RiakClient = RiakClient(system, "127.0.0.1", 8098)
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
  def store[T: RiakValueWriter](key: String, value: T): Future[Option[RiakValue]]

  // TODO: add support for storing without a key, putting the generated key into the RiakValue which it should then always produce.
  // def store(value: RiakValue): Future[String]
  // def store[T: RiakValueWriter](value: T): Future[String]

  def delete(key: String): Future[Unit]
}

case class BucketImpl(system: ActorSystem, httpConduit: ActorRef, name: String, resolver: ConflictResolver) extends Bucket {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.http.HttpHeaders.RawHeader
  import utils.SprayClientExtras._

  def fetch(key: String): Future[Option[RiakValue]] = {
    basicHttpRequest(Get(url(key))).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case MultipleChoices => resolveConflict(response, resolver)
        case NotFound        => None
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed("Fetch for key '%s' in bucket '%s' produced an unexpected response code '%s'.".format(key, name, other))
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def store[T: RiakValueWriter](key: String, value: T) = store(key, implicitly[RiakValueWriter[T]].write(value))
  def store(key: String, value: RiakValue): Future[Option[RiakValue]] = {
    // TODO: Add a nice, non-intrusive way to set query parameters, like 'returnbody', etc.

    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader("X-Riak-Vclock", vclock.toString))
    val request = addOptionalHeader(vclockHeader) ~> basicHttpRequest

    request(Put(url(key) + "?returnbody=true", value)).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case NoContent       => None
        case MultipleChoices => resolveConflict(response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed("Store for key '%s' in bucket '%s' produced an unexpected response code '%s'.".format(key, name, other))
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def delete(key: String): Future[Unit] = {
    basicHttpRequest(Delete(url(key))).map { response =>
      response.status match {
        case NoContent       => ()
        case NotFound        => ()
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed("Delete for key '%s' in bucket '%s' produced an unexpected response code '%s'.".format(key, name, other))
      }
    }
  }

  private val clientId = "%s".format(java.util.UUID.randomUUID())

  private def basicHttpRequest = {
    // TODO: make the client id optional based on some config (Settings in reference.conf)

    addHeader("X-Riak-ClientId", clientId) ~> sendReceive(httpConduit)
  }

  private def url(key: String) = "/buckets/%s/keys/%s".format(name, key)

  private def toRiakValue(response: HttpResponse): Option[RiakValue] = toRiakValue(response.entity, response.headers)
  private def toRiakValue(entity: HttpEntity, headers: List[HttpHeader]): Option[RiakValue] = {
    entity.toOption.flatMap { body =>
      import spray.http.HttpHeaders._

      val vClockOption       = headers.find(_.is("x-riak-vclock")).map(_.value)
      val eTagOption         = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified"))
                                      .map(h => new DateTime(h.asInstanceOf[`Last-Modified`].date.clicks))

      // TODO: make sure the DateTime is always in the Zulu zone

      for (vClock <- vClockOption; eTag <- eTagOption; lastModified <- lastModifiedOption)
      yield RiakValue(body.buffer, body.contentType, vClock, eTag, lastModified)
    }
  }

  private def resolveConflict(response: HttpResponse, resolver: ConflictResolver): Option[RiakValue] = {
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

        Some(value)
      }
    }
  }
}
