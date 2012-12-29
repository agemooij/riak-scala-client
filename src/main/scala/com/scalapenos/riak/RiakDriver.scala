package com.scalapenos.riak

import scala.concurrent.Future

import akka.actor._

import spray.json.RootJsonFormat

/*

val client = RiakClient()
val connection = client.connect()
val bucket = connection.bucket("test")

bucket

*/

// ============================================================================
// RiakClient
// ============================================================================

object RiakClient extends ExtensionId[RiakClient] with ExtensionIdProvider {
  def lookup() = RiakClient
  def createExtension(system: ExtendedActorSystem) = new RiakClient(system)
}

class RiakClient(system: ExtendedActorSystem) extends Extension {
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
  def bucket[T : RootJsonFormat](name: String): RiakBucket[T]
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

  def bucket[T : RootJsonFormat](name: String) = RiakBucketImpl(system, httpConduit, name)
}



// ============================================================================
// RiakBucket
// ============================================================================

abstract class RiakBucket[T : RootJsonFormat] {
  def fetch(key: String): Future[Option[T]]
  def store(key: String, value: T): Future[T]
  def delete(key: String): Future[Unit]
}

case class RiakBucketImpl[T : RootJsonFormat](system: ActorSystem, httpConduit: ActorRef, name: String) extends RiakBucket[T] {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.httpx._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  private def url(key: String) = "/buckets/%s/keys/%s".format(name, key)

  def fetch(key: String): Future[Option[T]] = {
    val pipeline = sendReceive(httpConduit)

    pipeline(Get(url(key))).map { response =>
      if (response.status.isSuccess)
        response.entity.as[T] match {
          case Right(value) => Some(value)
          case Left(error) => None
      } else None
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
}

