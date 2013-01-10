/*
 * Copyright (C) 2011-2012 scalapenos.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
