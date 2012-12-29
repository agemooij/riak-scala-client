package com.scalapenos.riak

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._

import spray.json._


class RiakDriverDataStoresSpec extends AkkaActorSystemSpecification {
  case class Kitten(name: String, cuteness: Int)
  object Kitten extends DefaultJsonProtocol {
    implicit val jsonFormat = jsonFormat2(Kitten.apply)
  }

  "The riak driver" should {
    "be able to perform a simple get-put-get-delete-get CRUD flow" in {
      val client = RiakClient(system)
      val connection = client.connect()
      val bucket = connection.bucket[Kitten]("test")

      Await.result(bucket.fetch("Nano"), 5 seconds) must beNone
    }
  }

}
