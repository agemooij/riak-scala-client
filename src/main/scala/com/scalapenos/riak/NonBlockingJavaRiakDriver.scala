package com.scalapenos.riak

import akka.actor._
import scala.concurrent._

import spray.json._

import com.basho.riak.client._
import com.basho.riak.client.bucket.Bucket
import com.basho.riak.client.builders.RiakObjectBuilder
import com.basho.riak.client.cap.VClock
import com.basho.riak.client.convert._
import com.basho.riak.client.http.util.Constants


class NonBlockingJavaRiakDriver(implicit system: ActorSystem) extends RiakDriver {
  private val client = RiakFactory.httpClient()

  def bucket[T : RootJsonFormat](name: String): RiakBucket[T] = new NonBlockingJavaRiakBucket[T](client.fetchBucket(name).execute())(system)
  def shutdown = client.shutdown
}

class NonBlockingJavaProtobufRiakDriver(implicit system: ActorSystem) extends RiakDriver {
  import system.dispatcher

  private val client = RiakFactory.pbcClient()

  def bucket[T : RootJsonFormat](name: String): RiakBucket[T] = new NonBlockingJavaRiakBucket[T](client.fetchBucket(name).execute())(system)
  def shutdown = client.shutdown
}

class NonBlockingJavaRiakBucket[T : RootJsonFormat](javaDriverBucket: Bucket)(system: ActorSystem) extends RiakBucket[T] {
  import system.dispatcher

  def get(key: String): Future[Option[T]] = {
    Future {
      Option(RiakConverter(key).toDomain(javaDriverBucket.fetch(key).execute()))
    }
  }

  def put(key: String, value: T): Future[T] = {
    Future {
      javaDriverBucket.store(key, value)
                      .withConverter(RiakConverter(key))
                      .returnBody(true)
                      .execute()
    }
  }

  def delete(key: String): Future[Unit] = {
    Future {
      javaDriverBucket.delete(key).execute()
    }
  }

  private case class RiakConverter(key: String) extends Converter[T] {
    def fromDomain(value: T, vClock: VClock): IRiakObject = {
      RiakObjectBuilder.newBuilder(javaDriverBucket.getName, key)
                       .withValue(value.toJson.compactPrint)
                       .withVClock(vClock)
                       .withContentType(Constants.CTYPE_JSON_UTF8)
                       .build()
    }

    def toDomain(value: IRiakObject): T = if (value == null) null.asInstanceOf[T]
                                          else value.getValueAsString.asJson.convertTo[T]
  }
}


