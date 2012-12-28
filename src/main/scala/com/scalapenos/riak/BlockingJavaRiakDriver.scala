package com.scalapenos.riak

import scala.concurrent._

import spray.json._

import com.basho.riak.client._
import com.basho.riak.client.bucket.Bucket
import com.basho.riak.client.builders.RiakObjectBuilder
import com.basho.riak.client.cap.VClock
import com.basho.riak.client.convert._
import com.basho.riak.client.http.util.Constants

class BlockingJavaRiakDriver extends RiakDriver {
  private val client = RiakFactory.httpClient()

  def bucket[T : RootJsonFormat](name: String): RiakBucket[T] = new BlockingJavaRiakBucket[T](client.fetchBucket(name).execute())
  def shutdown = client.shutdown
}

class BlockingJavaProtobufRiakDriver extends RiakDriver {
  private val client = RiakFactory.pbcClient()

  def bucket[T : RootJsonFormat](name: String): RiakBucket[T] = new BlockingJavaRiakBucket[T](client.fetchBucket(name).execute())
  def shutdown = client.shutdown
}

class BlockingJavaRiakBucket[T : RootJsonFormat](javaDriverBucket: Bucket) extends RiakBucket[T] {
  private implicit val ec = ExecutionContext.fromExecutorService(new java.util.concurrent.ForkJoinPool())

  def get(key: String): Future[Option[T]] = {
    Future.successful {
      Option(RiakConverter(key).toDomain(javaDriverBucket.fetch(key).execute()))
    }
  }

  def put(key: String, value: T): Future[T] = {
    Future.successful {
      javaDriverBucket.store(key, value)
                      .withConverter(RiakConverter(key))
                      .returnBody(true)
                      .execute()
    }
  }

  def delete(key: String): Future[Unit] = {
    Future.successful {
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


