package com.scalapenos.riak

import scala.concurrent.Future

import akka.actor._

import spray.http.ContentType

import org.joda.time.DateTime


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
}


// ============================================================================
// RiakConnection
// ============================================================================

// TODO: make the connection manage one single RiakConnectionActor, with its
//       own supervisor strategy for better fault tolerance. Getting a handle
//       on the bucket could be implemented using a reference to a child actor
//       wrapped in another trait

trait RiakConnection {
  def bucket(name: String): Bucket
}

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

  def bucket(name: String) = BucketImpl(system, httpConduit, name)
}



// ============================================================================
// Bucket
// ============================================================================

// TODO: Get rid of the T typing and provide unmarshalling at the RiakValue layer.
//       The driver should be type-agnostic
//       Fetch should return a Future[Option[RiakValue]]
//       Store should return a ...
//       Delete should return a ...
//       Resolvers should aso be type agnostic
//       RiakValue should have an as[T] method that uses the content type to
//         lookup an unmarshaller and convert the raw String value to a T

trait Bucket {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: move the Resolver to the bucket level too?

  def fetch(key: String)(implicit resolver: Resolver = LastValueWinsResolver): Future[Option[RiakValue]]

  // TODO: change this into any object that can be implicitly converted into a RiakValue
  def store(key: String, value: String): Future[Option[RiakValue]]

  // def store(key: String, value: RiakValue): Future[Option[RiakValue]]
  // def store[T: RiakValueMarshaller](key: String, value: T): Future[Option[RiakValue]]


  def delete(key: String): Future[Nothing]
}

case class BucketImpl(system: ActorSystem, httpConduit: ActorRef, name: String) extends Bucket {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  // TODO: all error situations should be thrown as exceptions so that they cause the future to fail
  //       That way we can simplify the RiakResponse/RiakValue hierarchy

  def fetch(key: String)(implicit resolver: Resolver = LastValueWinsResolver): Future[Option[RiakValue]] = {
    pipeline(Get(url(key))).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case MultipleChoices => resolveConflict(response, resolver)
        case NotFound        => None
        case BadRequest      => throw new FetchParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new FetchFailed("Unexpected response code '%s' on fetch".format(other))
      }
    }
  }

  // TODO: rewrite to match how fetch has been implemented
  def store(key: String, value: String): Future[Option[RiakValue]] = {
    throw new NotImplementedError("Store has not been implemented yet!")

    // import spray.httpx._

    // pipeline(Put(url(key) + "?returnbody=true", value)).map { response =>
    //   if (response.status.isSuccess)
    //     response.entity.as[T] match {
    //       case Right(value) => value
    //       case Left(error) => throw new PipelineException(error.toString)
    //   } else throw new UnsuccessfulResponseException(response.status)
    // }
  }

  // TODO: rewrite to match how fetch has been implemented
  def delete(key: String): Future[Nothing] = {
    throw new NotImplementedError("Delete has not been implemented yet!")

    //pipeline(Delete(url(key))).map(x => ())
  }


  private val clientId = "%s".format(java.util.UUID.randomUUID())

  private def pipeline = addHeader("X-Riak-ClientId", clientId) ~> sendReceive(httpConduit)

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
      yield RiakValue(entity.asString, body.contentType, vClock, eTag, lastModified)
    }
  }

  private def resolveConflict(response: HttpResponse, resolver: Resolver): Option[RiakValue] = {
    import spray.http._

    response.entity.as[MultipartContent] match {
      case Left(error) => throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) => {
        val values = multipartContent.parts.flatMap(part => toRiakValue(part.entity, part.headers)).toSet
        val value = resolver.resolve(values)

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // TODO: the resolved value needs to be stored back so the resolution sticks !!!!

        Some(value)
      }
    }
  }
}


// ============================================================================
// Resolving multiple fetch values
// ============================================================================

// TODO: Rename to ConflictResolver?

trait Resolver {
  def resolve(values: Set[RiakValue]): RiakValue
}

case object LastValueWinsResolver extends Resolver {
  def resolve(values: Set[RiakValue]): RiakValue = {
    values.reduceLeft { (first, second) =>
      if (second.lastModified.isAfter(first.lastModified)) second
      else first
    }
  }
}


// ============================================================================
// RiakValue
// ============================================================================

// TODO: values should be byte arrays, with lots of handy stuff to submit
//       Strings or anything else that can be converted to/from a byte array using
//       a RiakValueMarshaller/RiakValueUnmarshaller

// Should unmarshallers be RiakValue => T or Array[Byte] => T ?
//   Probably the first, since it just adds some extra information

// It's probably a good idea to define a VClock type so we can easily create
// a common empty one that denotes the case when no vclock information is
// available.

final case class RiakValue(
  value: String,
  contentType: ContentType,
  vclock: String,
  etag: String,
  lastModified: DateTime
  // links: Seq[RiakLink]
  // meta: Seq[RiakMeta]
) {

  // TODO: add as[T: RiakValueUnmarshaller] function linked to the ContentType

  // TODO: add common manipulation functions
}

case class FetchFailed(cause: String) extends RuntimeException(cause)
case class FetchParametersInvalid(cause: String) extends RuntimeException(cause)
case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
