package com.scalapenos.riak

import akka.actor._
import scala.concurrent.Future

import spray.json._


abstract class RiakDriver() {
  def bucket[T : RootJsonFormat](name: String): RiakBucket[T]
  def shutdown
}

abstract class RiakBucket[T : RootJsonFormat] {
  def get(key: String): Future[Option[T]]
  def put(key: String, value: T): Future[T]
  def delete(key: String): Future[Unit]
}

