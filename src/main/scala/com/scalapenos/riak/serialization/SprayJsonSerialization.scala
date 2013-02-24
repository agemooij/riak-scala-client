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
package serialization

import scala.reflect.ClassTag
import scala.util._

import spray.http.ContentType
import spray.http.MediaType
import spray.http.MediaTypes._
import spray.json._


trait SprayJsonSerialization {
  class SprayJsonSerializer[T: RootJsonWriter] extends RiakSerializer[T] {
    def serialize(t: T) = (implicitly[RootJsonWriter[T]].write(t).compactPrint, ContentType.`application/json`)
  }

  class SprayJsonDeserializer[T: RootJsonReader: ClassTag] extends RiakDeserializer[T] {
    def deserialize(data: String, contentType: ContentType) = {
      contentType match {
        case ContentType(`application/json`, _) => parseAndConvert(data)
        case _ => Failure(RiakUnsupportedContentTypeException(ContentType.`application/json`, contentType))
      }
    }

    private def parseAndConvert(data: String) = {
        Try(implicitly[RootJsonReader[T]].read(JsonParser(data))) recoverWith {
          case throwable => Failure(RiakDeserializationFailedException(data, implicitly[ClassTag[T]].runtimeClass.getName, throwable))
        }
    }
  }

  implicit def sprayJsonSerializer[T: RootJsonFormat] = new SprayJsonSerializer[T]
  implicit def sprayJsonDeserializer[T: RootJsonFormat: ClassTag] = new SprayJsonDeserializer[T]
}

object SprayJsonSerialization extends SprayJsonSerialization
