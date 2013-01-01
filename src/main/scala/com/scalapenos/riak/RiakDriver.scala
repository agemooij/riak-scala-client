package com.scalapenos.riak

import scala.concurrent.Future

import akka.actor._

import spray.json.RootJsonFormat

/*

val client = Riak()
val connection = client.connect()
val bucket = connection.bucket("test")

bucket

*/

// ============================================================================
// Riak
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
  import spray.http._
  import spray.http.StatusCodes._
  import spray.httpx._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  private def url(key: String) = "/buckets/%s/keys/%s".format(name, key)

  def fetch(key: String)(implicit resolver: Resolver[T] = LastValueWinsResolver()): Future[FetchResponse[T]] = {
    val pipeline = sendReceive(httpConduit)

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
    val pipeline = sendReceive(httpConduit)

    pipeline(Put(url(key) + "?returnbody=true", value)).map { response =>
      if (response.status.isSuccess)
        response.entity.as[T] match {
          case Right(value) => value
          case Left(error) => throw new PipelineException(error.toString)
      } else throw new UnsuccessfulResponseException(response.status)
    }
  }

  def delete(key: String): Future[Unit] = {
    val pipeline = sendReceive(httpConduit)

    pipeline(Delete(url(key))).map(x => ())
  }


  private def fetchedValue(response: HttpResponse): FetchResponse[T] = {
    import com.github.nscala_time.time.Imports._

    response.entity.as[T] match {
      case Right(value) => FetchedValue(value, "", "", DateTime.now) // TODO: fill in the header-derived values
      case Left(error) => FetchValueUnmarshallingFailed(error.toString)
    }
  }

  private def resolveValues(response: HttpResponse, resolver: Resolver[T]): FetchResponse[T] = {
    // TODO: implement me!!

    FetchError("Resolving multiple values using a resolver has not been implemented yet.")
  }
}


// ============================================================================
// Resolving multiple fetch values
// ============================================================================

// TODO: resolvers should take a set of FetchedValue[T]s, so that they can look at vector clocks, timestamps, etc.

trait Resolver[T] {
  def resolve(values: Set[FetchedValue[T]]): FetchedValue[T]
}

case class LastValueWinsResolver[T]() extends Resolver[T] {
  def resolve(values: Set[FetchedValue[T]]) = {
    values.reduceLeft { (first, second) =>
      if (second.lastModified.isAfter(first.lastModified)) second
      else first
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


import com.github.nscala_time.time.Imports._

sealed abstract class FetchResponse[+A] extends RiakResponse {
  def value: A
}

final case class FetchedValue[+A](
  value: A,
  vclock: String,
  etag: String,
  lastModified: DateTime
  // links: Seq[RiakLink]
  // meta: Seq[RiakMeta]
) extends FetchResponse[A] with RiakSuccess {
  // TODO: the usual Monad stuff, like map, flatmap, filter
}


sealed abstract class FetchFailed extends FetchResponse[Nothing] with RiakFailure {
  def value = throw new NoSuchElementException("RiakFailure.value")
}

case object FetchKeyNotFound extends FetchFailed
case class  FetchParametersInvalid(reason: String) extends FetchFailed
case class  FetchValueUnmarshallingFailed(cause: String) extends FetchFailed
case class  FetchError(cause: String) extends FetchFailed

