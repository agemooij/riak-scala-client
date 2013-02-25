/*
 * Copyright (C) 2012-2013 Age Mooij (http://scalapenos.com)
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
package serialization

import org.specs2.mutable._
import scala.util._


class SprayJsonSerializationSpec extends Specification {
  import spray.json.DefaultJsonProtocol._
  import spray.http.MediaTypes._
  import SprayJsonSerialization._

  case class Thingy(name: String, number: Int)
  object Thingy {
    implicit val jsonFormat = jsonFormat2(Thingy.apply)
  }

  val validJson = """{"name":"Answer","number":42}"""
  val invalidJson = """{"name": "Answer"}"""

  "SprayJsonDeserializer.deserialize(...)" should {
    "correctly deserialize valid JSON when the ContentType is ContentType.`application/json`." in {
      val thingy = implicitly[RiakDeserializer[Thingy]].deserialize(validJson, ContentType.`application/json`)

      thingy.name must beEqualTo("Answer")
      thingy.number must beEqualTo(42)
    }

    "correctly deserialize valid JSON when the ContentType is ContentType(`application/json`)." in {
      val thingy = implicitly[RiakDeserializer[Thingy]].deserialize(validJson, ContentType(`application/json`))

      thingy.name must beEqualTo("Answer")
      thingy.number must beEqualTo(42)
    }

    "fail when deserializing with ContentType.`application/json` but invalid JSON data" in {
      val deserializer = implicitly[RiakDeserializer[Thingy]]

      deserializer.deserialize(invalidJson, ContentType.`application/json`) must throwA[RiakDeserializationFailed].like {
        case exception: RiakDeserializationFailed => {
          exception.data must beEqualTo(invalidJson)
          exception.targetType must beEqualTo(classOf[Thingy].getName)
          exception.cause must beAnInstanceOf[spray.json.DeserializationException]
        }
      }
    }

    "fail when deserializing with an unsupported ContentType" in {
      val deserializer = implicitly[RiakDeserializer[Thingy]]

      deserializer.deserialize(validJson, ContentType.`text/plain`) must throwA(RiakUnsupportedContentType(ContentType.`application/json`, ContentType.`text/plain`))
    }
  }

  "SprayJsonSerializer.serialize(T)" should {
    "correctly convert T to a JSON string and ContentType.`application/json`" in {
      val thingy = new Thingy("Answer", 42)
      val (data, contentType) = implicitly[RiakSerializer[Thingy]].serialize(thingy)

      data must beEqualTo(validJson)
      contentType must beEqualTo(ContentType.`application/json`)
    }
  }

}
