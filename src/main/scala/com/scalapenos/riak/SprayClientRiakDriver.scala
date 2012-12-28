package com.scalapenos.riak

import akka.actor._
import scala.concurrent._

import spray.json._


object SprayClientRiakDriver {
  def apply(system: ActorSystem = ActorSystem("spray-client-riak-driver")) = new SprayClientRiakDriver(system)
}

class SprayClientRiakDriver(system: ActorSystem) extends RiakDriver {
  import spray.can.client.HttpClient
  import spray.client._
  import spray.io.IOExtension

  private val ioBridge = IOExtension(system).ioBridge()
  private val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))

  // TODO: add Settings object and read host/port/etc from that
  private val httpConduit = system.actorOf(Props(new HttpConduit(
    httpClient = httpClient,
    host = "localhost",
    port = 8098,
    dispatchStrategy = DispatchStrategies.Pipelined
  )))

  def bucket[T : RootJsonFormat](name: String): RiakBucket[T] = new SprayClientRiakBucket[T](system, name, httpConduit)
  def shutdown = {}
}

class SprayClientRiakBucket[T : RootJsonFormat](system: ActorSystem, name: String, httpConduit: ActorRef) extends RiakBucket[T] {
  import system.dispatcher
  import spray.client.HttpConduit._
  import spray.httpx._
  import spray.httpx.unmarshalling._
  import spray.httpx.SprayJsonSupport._

  private def url(key: String) = "/buckets/%s/keys/%s".format(name, key)

  def get(key: String): Future[Option[T]] = {
    val pipeline = sendReceive(httpConduit)

    pipeline(Get(url(key))).map { response =>
      if (response.status.isSuccess)
        response.entity.as[T] match {
          case Right(value) => Some(value)
          case Left(error) => None
      } else None
    }
  }

  def put(key: String, value: T): Future[T] = {
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


