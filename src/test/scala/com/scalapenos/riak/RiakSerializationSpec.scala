/*
 * Copyright (C) 2012-2013 Age Mooij
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
import spray.http.MediaTypes._


class RiakSerializationSpec extends Specification {

  case class ClassWithoutCustomSerialization(a: String, b: Int)
  case class ClassWithCustomSerialization(a: String, b: Int, c: Boolean)

  object ClassWithCustomSerialization {
    implicit def companionXmlSerializer = new RiakSerializer[ClassWithCustomSerialization] {
      def serialize(t: ClassWithCustomSerialization): (String, ContentType) = {
        (<xml><a>{t.a}</a><b>{t.b}</b><c>{t.c}</c></xml>.toString, ContentType(`text/xml`))
      }
    }
  }

  object CustomJsonSerialization {
    implicit def jsonSerializer = new RiakSerializer[ClassWithCustomSerialization] {
      def serialize(t: ClassWithCustomSerialization): (String, ContentType) = {
        (s"""{a: "${t.a}", b: ${t.b}, c: ${t.c}""", ContentType(`application/json`))
      }
    }
  }


  "When serializing any type T, it" should {
    "serialize to (t.toString, ContentType.`text/plain`) if DefaultRiakSerializationSupport._ is imported" in {
      import DefaultRiakSerializationSupport._

      val t = new ClassWithoutCustomSerialization("The answer", 42)

      val (data, contentType) = implicitly[RiakSerializer[ClassWithoutCustomSerialization]].serialize(t)

      data must beEqualTo(t.toString)
      contentType must beEqualTo(ContentType.`text/plain`)
    }

    "serialize using a Serializer[T] defined in the companion object of T" in {
      val t = new ClassWithCustomSerialization("The answer", 42, true)

      val (data, contentType) = implicitly[RiakSerializer[ClassWithCustomSerialization]].serialize(t)

      data must beEqualTo(s"<xml><a>${t.a}</a><b>${t.b}</b><c>${t.c}</c></xml>")
      contentType must beEqualTo(ContentType(`text/xml`))
    }

    "serialize using an imported Serializer[T], preferring it over the serializer defined in the companion object of T" in {
      import CustomJsonSerialization._

      val t = new ClassWithCustomSerialization("The answer", 42, true)

      val (data, contentType) = implicitly[RiakSerializer[ClassWithCustomSerialization]].serialize(t)

      data must beEqualTo(s"""{a: "${t.a}", b: ${t.b}, c: ${t.c}""")
      contentType must beEqualTo(ContentType(`application/json`))
    }
  }

}
