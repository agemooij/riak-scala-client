package com.scalapenos.riak
package converters

import org.specs2.mutable._
import scala.util._

import akka.actor._
import spray.http.ContentType

import com.github.nscala_time.time.Imports._


/**
 * This test depends on a Riak node running on localhost:8098 !!
 */
class SprayJsonRiakValueConverterSpec extends Specification {
  import spray.json.DefaultJsonProtocol._
  import SprayJsonRiakValueConverter._

  case class Thingy(name: String, number: Int)
  object Thingy {
    implicit val jsonFormat = jsonFormat2(Thingy.apply)
  }

  val validJson = """{"name":"Answer","number":42}"""
  val invalidJson = """{"name": "Answer"}"""

  "SprayJsonConverter.read(RiakValue)" should {
    "correctly convert a RiakValue with ContentType `application/json` and valid JSON" in {
      val riakValue = RiakValue(validJson, ContentType.`application/json`, "vclock123", "etag123", DateTime.now)
      val thingy = implicitly[RiakValueConverter[Thingy]].read(riakValue)

      thingy must beAnInstanceOf[Success[Thingy]]
      thingy.foreach{ t =>
        t.name must beEqualTo("Answer")
        t.number must beEqualTo(42)
      }
    }

    "fail when converting a RiakValue with ContentType `application/json` but invalid JSON" in {
      val riakValue = RiakValue(invalidJson, ContentType.`application/json`, "vclock123", "etag123", DateTime.now)
      val thingy = implicitly[RiakValueConverter[Thingy]].read(riakValue)

      thingy must beAnInstanceOf[Failure[Thingy]]

      val exception = thingy.asInstanceOf[Failure[Thingy]].exception

      exception must beAnInstanceOf[ConversionFailedException]
    }

    "fail when converting a RiakValue with an unsupported ContentType" in {
      val riakValue = RiakValue(validJson, ContentType.`text/plain`, "vclock123", "etag123", DateTime.now)
      val thingy = implicitly[RiakValueConverter[Thingy]].read(riakValue)

      thingy must beAnInstanceOf[Failure[Thingy]]

      val exception = thingy.asInstanceOf[Failure[Thingy]].exception

      exception must beAnInstanceOf[ConversionFailedException]
    }
  }

  "SprayJsonConverter.write(T)" should {
    "correctly convert T to a RiakValue with ContentType `application/json`" in {
      val thingy = new Thingy("Answer", 42)
      val value = implicitly[RiakValueConverter[Thingy]].write(thingy)

      value.asString must beEqualTo(validJson)
      value.contentType must beEqualTo(ContentType.`application/json`)
    }
  }

}
