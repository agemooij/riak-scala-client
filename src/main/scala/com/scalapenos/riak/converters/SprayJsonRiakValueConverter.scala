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
package converters

import scala.reflect.ClassTag
import scala.util._

import spray.http.ContentType
import spray.http.MediaType
import spray.http.MediaTypes._
import spray.json._


class SprayJsonRiakValueConverter[T: RootJsonFormat: ClassTag] extends RiakValueConverter[T] {
  def contentType = ContentType.`application/json`
  def serialize(t: T) = implicitly[RootJsonFormat[T]].write(t).compactPrint

  def read(riakValue: RiakValue): Try[T] = {
    riakValue.contentType match {
      case ContentType(`application/json`, _) => {
        val jsonFormat = implicitly[RootJsonFormat[T]]

        Try(jsonFormat.read(riakValue.data.asJson)) recoverWith {
          case e => Failure(ConversionFailedException(
                      "The JSON contained in the RiakValue cannot be used to produce an instance of %s".format(
                        implicitly[ClassTag[T]].runtimeClass.getSimpleName), e))
        }
      }

      case _ => Failure(
        new ConversionFailedException(
          "The ContentType of the RiakValue is `%s` instead of the `application/json` required by SprayJsonConverter".format(
            riakValue.contentType, implicitly[ClassTag[T]].runtimeClass)))
    }
  }

}

object SprayJsonRiakValueConverter {
  implicit def sprayJsonRiakValueConverter[T: RootJsonFormat: ClassTag] = new SprayJsonRiakValueConverter[T]
}
