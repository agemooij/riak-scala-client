package com.scalapenos.riak

import scala.concurrent.Future

import akka.actor._

import spray.http.ContentType

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
}


// ============================================================================
// RiakConnection
// ============================================================================

// TODO: make the connection manage one single RiakConnectionActor, with its
//       own supervisor strategy for better fault tolerance. Getting a handle
//       on the bucket could be implemented using a reference to a child actor
//       wrapped in another trait

trait RiakConnection {
  def bucket(name: String, resolver: ConflictResolver = LastValueWinsResolver): Bucket
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

  def bucket(name: String, resolver: ConflictResolver) = BucketImpl(system, httpConduit, name, resolver)
}


// ============================================================================
// Bucket
// ============================================================================

abstract class Bucket(resolver: ConflictResolver) {
  // TODO: add Retry support, maybe at the bucket level
  // TODO: use URL-escaping to make sure all keys (and bucket names) are valid

  def fetch(key: String): Future[Option[RiakValue]]

  def store(key: String, value: RiakValue): Future[Option[RiakValue]]
  // TODO: change this into any object that can be implicitly converted into a RiakValue
  def store(key: String, value: String): Future[Option[RiakValue]]
  // def store[T: RiakValueMarshaller](key: String, value: T): Future[Option[RiakValue]]

  def delete(key: String): Future[Nothing]
}

case class BucketImpl(system: ActorSystem, httpConduit: ActorRef, name: String, resolver: ConflictResolver) extends Bucket(resolver) {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  // TODO: all error situations should be thrown as exceptions so that they cause the future to fail
  //       That way we can simplify the RiakResponse/RiakValue hierarchy

  def fetch(key: String): Future[Option[RiakValue]] = {
    pipeline(Get(url(key))).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case MultipleChoices => resolveConflict(response, resolver)
        case NotFound        => None
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new FetchFailed("Unexpected response code '%s' on fetch".format(other))
      }
    }
  }


  def store(key: String, value: String): Future[Option[RiakValue]] = store(key, RiakValue(value))
  def store(key: String, value: RiakValue): Future[Option[RiakValue]] = {
    // TODO: set vclock header

    pipeline(Put(url(key) + "?returnbody=true", value)).map { response =>
      response.status match {
        case OK              => toRiakValue(response)
        case NoContent       => None
        case MultipleChoices => resolveConflict(response, resolver)
        case BadRequest      => throw new ParametersInvalid("Does Riak even give us a reason for this?")
        case other           => throw new FetchFailed("Unexpected response code '%s' on fetch".format(other))
        // case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
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
      yield RiakValue(body.buffer, body.contentType, vClock, eTag, lastModified)
    }
  }

  private def resolveConflict(response: HttpResponse, resolver: ConflictResolver): Option[RiakValue] = {
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

trait ConflictResolver {
  def resolve(values: Set[RiakValue]): RiakValue
}

case object LastValueWinsResolver extends ConflictResolver {
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

// It's probably a good idea to define a VClock (implicit) value class so we can easily create
// a common empty one that denotes the case when no vclock information is
// available. The same goes for eTags.

final case class RiakValue(
  value: Array[Byte],
  contentType: ContentType,
  vclock: String,
  etag: String,
  lastModified: DateTime
  // links: Seq[RiakLink]
  // meta: Seq[RiakMeta]
) {

  def asString = new String(value, contentType.charset.nioCharset)

  // TODO: add as[T: RiakValueUnmarshaller] function linked to the ContentType

  // TODO: add common manipulation functions
}

object RiakValue {
  def apply(value: String): RiakValue = {
    val contentType = ContentType.`text/plain`

    new RiakValue(
      value.getBytes(contentType.charset.nioCharset),
      contentType,
      "",
      "",
      DateTime.now
    )
  }

  import spray.http.HttpBody
  import spray.httpx.marshalling._
  implicit val RiakValueMarshaller: Marshaller[RiakValue] = new Marshaller[RiakValue] {
    def apply(riakValue: RiakValue, ctx: MarshallingContext) {
      ctx.marshalTo(HttpBody(riakValue.contentType, riakValue.value))
    }
  }
}


// ============================================================================
// Exceptions
// ============================================================================

case class FetchFailed(cause: String) extends RuntimeException(cause)
case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
case class ParametersInvalid(cause: String) extends RuntimeException(cause)

