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

import scala.util._
import org.specs2.mutable._
import MediaTypes._


class RiakSerializationSpec extends Specification {

  case class ClassWithoutCustomSerialization(a: String, b: Int)
  case class ClassWithCustomSerialization(a: String)

  object ClassWithCustomSerialization {
    implicit def companionXmlSerializer = new RiakSerializer[ClassWithCustomSerialization] {
      def serialize(t: ClassWithCustomSerialization): (String, ContentType) = {
        (<xml><a>{t.a}</a></xml>.toString, ContentType(`text/xml`))
      }
    }

    implicit def companionXmlDeserializer = new RiakDeserializer[ClassWithCustomSerialization] {
      import scala.xml._
      def deserialize(data: String, contentType: ContentType): ClassWithCustomSerialization = {
        def toXml = XML.load(new java.io.StringReader(data))
        def fromXml(elem: Elem) = ClassWithCustomSerialization((elem \ "a").text)

        contentType match {
          case ContentType(`text/xml`, _) => fromXml(toXml)
          case _ => throw RiakUnsupportedContentType(ContentType(`text/xml`), contentType)
        }
      }
    }
  }

  object CustomJsonSerialization {
    implicit def jsonSerializer = new RiakSerializer[ClassWithCustomSerialization] {
      def serialize(t: ClassWithCustomSerialization): (String, ContentType) = {
        (s"""{a: "${t.a}"}""", ContentType(`application/json`))
      }
    }
  }

  object CustomJsonFormatForClassWithoutCustomSerialization {
    import spray.json.DefaultJsonProtocol._

    implicit def jsonFormat = jsonFormat2(ClassWithoutCustomSerialization)
  }

  "When serializing any type T, it" should {
    "serialize using a Serializer[T] defined in the companion object of T" in {
      val t = new ClassWithCustomSerialization("The answer is 42")

      val (data, contentType) = implicitly[RiakSerializer[ClassWithCustomSerialization]].serialize(t)

      data must beEqualTo(s"<xml><a>${t.a}</a></xml>")
      contentType must beEqualTo(ContentType(`text/xml`))
    }

    "serialize using an imported Serializer[T], preferring it over the serializer defined in the companion object of T" in {
      import CustomJsonSerialization._

      val t = new ClassWithCustomSerialization("The answer is 42")

      val (data, contentType) = implicitly[RiakSerializer[ClassWithCustomSerialization]].serialize(t)

      data must beEqualTo(s"""{a: "${t.a}"}""")
      contentType must beEqualTo(ContentType(`application/json`))
    }
  }

  "When deserializing (String, ContentType) to any type T, it" should {
    "deserialize to Success(ClassWithCustomSerialization) if the ContentType matches the one defined in the RiakDeserializer" in {
      val data = """<xml><a>w00t!</a></xml>"""
      val out = implicitly[RiakDeserializer[ClassWithCustomSerialization]].deserialize(data, ContentType(`text/xml`))

      out must beEqualTo(ClassWithCustomSerialization("w00t!"))
    }

    "deserialize to Failure(UnsupportedContentTypeException) if the ContentType doesn't match the one defined in the RiakDeserializer" in {
      val data = """<xml><a>w00t!</a></xml>"""
      val deserializer = implicitly[RiakDeserializer[ClassWithCustomSerialization]]

      deserializer.deserialize(data, ContentType(`application/json`)) must throwA(RiakUnsupportedContentType(ContentType(`text/xml`), ContentType(`application/json`)))
    }
  }

  "By default, without any imports, it" should {
    "serialize Strings to text/plain" in {
      val anyString = "is a good String"
      val (data, contentType) = implicitly[RiakSerializer[String]].serialize(anyString)

      data must beEqualTo(anyString)
      contentType must beEqualTo(ContentType.`text/plain`)
    }

    "deserialize to the raw string data" in {
      val data = "some string"
      val out = implicitly[RiakDeserializer[String]].deserialize(data, ContentType.`text/plain`)

      out must beEqualTo(data)
    }

    "deserialize to the raw string data, ignoring the ContentType" in {
      val data = """{some: "string"}"""
      val out = implicitly[RiakDeserializer[String]].deserialize(data, ContentType(`application/json`))

      out must beEqualTo(data)
    }

    "serialize using any defined RootJsonWriter[T] in scope" in {
      import CustomJsonFormatForClassWithoutCustomSerialization._

      val t = new ClassWithoutCustomSerialization("The answer is", 42)

      val (data, contentType) = implicitly[RiakSerializer[ClassWithoutCustomSerialization]].serialize(t)

      data must beEqualTo(s"""{"a":"${t.a}","b":${t.b}}""")
      contentType must beEqualTo(ContentType.`application/json`)
    }

    "deserialize using any defined RootJsonReader[T] in scope" in {
      import CustomJsonFormatForClassWithoutCustomSerialization._

      val data = s"""{"a":"The answer is","b":42}"""
      val out = implicitly[RiakDeserializer[ClassWithoutCustomSerialization]].deserialize(data, ContentType(`application/json`))

      out must beEqualTo(new ClassWithoutCustomSerialization("The answer is", 42))
    }
  }

}
