package com.scalapenos.riak

import scala.concurrent.Future
import akka.actor._
import com.github.nscala_time.time.Imports._


// ============================================================================
// Riak (the main entry point)
// ============================================================================

object Riak extends ExtensionId[Riak] with ExtensionIdProvider {
  def lookup() = Riak
  def createExtension(system: ExtendedActorSystem) = new Riak(system)
}

class Riak(system: ExtendedActorSystem) extends Extension {
  def this() = this(ActorSystem("riak-scala-driver").asInstanceOf[ExtendedActorSystem])

  def connect(): RiakConnection = connect("127.0.0.1", 8098)
  def connect(host: String, port: Int): RiakConnection = RiakConnectionImpl(system, host, port)

  // TODO: how to deal with:
  //       - Shutting down the ActorSystem when we're done and we created the actor system to begin with)
  //       - someone else shutting down the ActorSystem, leaving us in an invalid state
}


// ============================================================================
// The API Definitions
// ============================================================================

trait RiakConnection {
  import resolvers.LastValueWinsResolver

  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): Bucket
}

abstract class Bucket(resolver: ConflictResolver) {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: use URL-escaping to make sure all keys (and bucket names) are valid

  def fetch(key: String): Future[Option[RiakValue]]

  def store(key: String, value: RiakValue): Future[Option[RiakValue]]
  // TODO: change this into any object that can be implicitly converted into a RiakValue
  def store(key: String, value: String): Future[Option[RiakValue]]
  // def store[T: RiakValueMarshaller](key: String, value: T): Future[Option[RiakValue]]

  def delete(key: String): Future[Unit]
}


// ============================================================================
// RiakConnectionImpl
// ============================================================================

// TODO: make the connection manage one single RiakConnectionActor, with its
//       own supervisor strategy for better fault tolerance. Getting a handle
//       on the bucket could be implemented using a reference to a child actor
//       wrapped in another trait

case class RiakConnectionImpl(system: ExtendedActorSystem, host: String, port: Int) extends RiakConnection {
  import spray.can.client.HttpClient
  import spray.client._
  import spray.io.IOExtension

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


// ============================================================================
// BucketImpl
// ============================================================================

case class BucketImpl(system: ActorSystem, httpConduit: ActorRef, name: String, resolver: ConflictResolver) extends Bucket(resolver) {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._

  def fetch(key: String): Future[Option[RiakValue]] = {
    basicHttpRequest(Get(url(key))).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case MultipleChoices => resolveConflict(response, resolver)
        case NotFound        => None
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed("Fetch for key '%s' in bucket '%s' produced an unexpected response code '%s'.".format(key, name, other))
      }
    }
  }

  def store(key: String, value: String): Future[Option[RiakValue]] = store(key, RiakValue(value))
  def store(key: String, value: RiakValue): Future[Option[RiakValue]] = {
    // TODO: Add a nice, non-intrusive way to set query parameters, like 'returnbody', etc.

    val request = if (value.vclock.isDefined) addHeader("X-Riak-Vclock", value.vclock.toString) ~> basicHttpRequest
                  else basicHttpRequest

    request(Put(url(key) + "?returnbody=true", value)).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case NoContent       => None
        case MultipleChoices => resolveConflict(response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new BucketOperationFailed("Store for key '%s' in bucket '%s' produced an unexpected response code '%s'.".format(key, name, other))
        // case PreconditionFailed => ... // needed when we support conditional request semantics
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
