package com.scalapenos.riak

import scala.concurrent.Future

import akka.actor._

import spray.json.RootJsonFormat

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
  def bucket[T : RootJsonFormat](name: String): Bucket[T]
}

case class RiakConnectionImpl(system: ExtendedActorSystem, host: String, port: Int) extends RiakConnection {
  import spray.can.client.HttpClient
  import spray.client._
  import spray.io.IOExtension

  // TODO: set up a riak client id (use a GUID)

  private[this] val ioBridge = IOExtension(system).ioBridge()
  private[this] val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))
  private[this] val httpConduit = system.actorOf(Props(new HttpConduit(
    httpClient = httpClient,
    host = host,
    port = port,
    dispatchStrategy = DispatchStrategies.Pipelined // TODO: read this from the settings
  )))

  def bucket[T : RootJsonFormat](name: String) = BucketImpl(system, httpConduit, name)
}



// ============================================================================
// Bucket
// ============================================================================

abstract class Bucket[T : RootJsonFormat] {
  def fetch(key: String)(implicit resolver: Resolver[T] = LastValueWinsResolver()): Future[FetchResponse[T]]
  def store(key: String, value: T): Future[T] // Future[StoreResponse[T]]
  def delete(key: String): Future[Unit] // Future[DeleteResponse[T]]
}

case class BucketImpl[T : RootJsonFormat](system: ActorSystem, httpConduit: ActorRef, name: String) extends Bucket[T] {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.httpx._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  private val clientId = "%s".format(java.util.UUID.randomUUID())

  def fetch(key: String)(implicit resolver: Resolver[T] = LastValueWinsResolver()): Future[FetchResponse[T]] = {
    pipeline(Get(url(key))).map { response =>
      response.status match {
        case OK              => fetchedValue(response)
        case MultipleChoices => resolveValues(response, resolver)
        case NotFound        => FetchKeyNotFound
        case BadRequest      => FetchParametersInvalid("Does Riak even give us a reason for this?")
        case other           => FetchError("Unexpected response code '%s' on fetch".format(other))
      }
    }
  }

  def store(key: String, value: T): Future[T] = {
    pipeline(Put(url(key) + "?returnbody=true", value)).map { response =>
      if (response.status.isSuccess)
        response.entity.as[T] match {
          case Right(value) => value
          case Left(error) => throw new PipelineException(error.toString)
      } else throw new UnsuccessfulResponseException(response.status)
    }
  }

  def delete(key: String): Future[Unit] = {
    pipeline(Delete(url(key))).map(x => ())
  }


  private def pipeline = {
    addHeader("X-Riak-ClientId", clientId) ~> sendReceive(httpConduit)
  }

  private def url(key: String) = "/buckets/%s/keys/%s".format(name, key)

  private def fetchedValue(response: HttpResponse): FetchResponse[T] = fetchedValue(response.entity, response.headers)
  private def fetchedValue(entity: HttpEntity, headers: List[HttpHeader]): FetchResponse[T] = {
    entity.as[T] match {
      case Left(error) => FetchValueUnmarshallingFailed(error.toString)
      case Right(value) => {
        import spray.http.HttpHeaders._

        val vClock = headers.find(_.is("x-riak-vclock")).map(_.value)
        val eTag = headers.find(_.is("etag")).map(_.value)
        val lastModified = headers.find(_.is("last-modified"))
                                  .map(h => new DateTime(h.asInstanceOf[`Last-Modified`].date.clicks))

        FetchedValue(value, vClock, eTag, lastModified)
      }
    }
  }

  private def resolveValues(response: HttpResponse, resolver: Resolver[T]): FetchResponse[T] = {
    import spray.http._

    response.entity.as[MultipartContent] match {
      case Left(error) => FetchValueUnmarshallingFailed(error.toString)
      case Right(multipartContent) => {
        val values = multipartContent.parts.toSet.flatMap { part: BodyPart =>
          fetchedValue(part.entity, part.headers) match {
            case value: FetchedValue[T] => Some(value)
            case _                      => None
          }
        }

        println("===============================================================================")
        values.foreach(println)
        println("===============================================================================")

        resolver.resolve(values)
      }
    }
  }
}


// ============================================================================
// Resolving multiple fetch values
// ============================================================================

trait Resolver[T] {
  def resolve(values: Set[FetchedValue[T]]): FetchedValue[T]
}

case class LastValueWinsResolver[T]() extends Resolver[T] {
  def resolve(values: Set[FetchedValue[T]]) = {
    values.reduceLeft { (first, second) =>
      (for (f <- first.lastModified; s <- second.lastModified)
       yield (if (s.isAfter(f)) second else first)).getOrElse(second)
    }
  }
}



// ============================================================================
// Responses
// ============================================================================

sealed trait RiakResponse {
  def isSuccess: Boolean
  def isFailure = !isSuccess
}

sealed trait RiakSuccess extends RiakResponse {
  def isSuccess = true
}

sealed trait RiakFailure extends RiakResponse {
  def isSuccess = false
}

// sealed abstract class RiakValue[+A] extends RiakSuccess {
//   def value: A
//   def vclock: Option[String]
//   def etag: Option[String]
//   def lastModified: Option[DateTime]
//   // def links: Seq[RiakLink]
//   // def meta: Seq[RiakMeta]

//   // TODO: the usual Monad stuff, like map, flatmap, filter
// }


// ============================================================================
// Fetch Responses
// ============================================================================

sealed abstract class FetchResponse[+A] extends RiakResponse {
  def value: A
}

final case class FetchedValue[+A](
  value: A,
  vclock: Option[String],
  etag: Option[String],
  lastModified: Option[DateTime]
  // links: Seq[RiakLink]
  // meta: Seq[RiakMeta]
) extends FetchResponse[A] with RiakSuccess {

}


sealed abstract class FetchFailed extends FetchResponse[Nothing] with RiakFailure {
  def value = throw new NoSuchElementException("RiakFailure.value")
}

case object FetchKeyNotFound extends FetchFailed
case class  FetchParametersInvalid(reason: String) extends FetchFailed
case class  FetchValueUnmarshallingFailed(cause: String) extends FetchFailed
case class  FetchError(cause: String) extends FetchFailed


// ============================================================================
// Store Responses
// ============================================================================

sealed abstract class StoreResponse[+A] extends RiakResponse

case object StoreSuccessful extends StoreResponse[Nothing] with RiakSuccess

final case class StoredValue[+A](
  value: A,
  vclock: Option[String],
  etag: Option[String],
  lastModified: Option[DateTime]
  // links: Seq[RiakLink]
  // meta: Seq[RiakMeta]
) extends FetchResponse[A] with RiakSuccess {

}