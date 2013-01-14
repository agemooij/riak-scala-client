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

import scala.reflect.ClassTag
import scala.util._

import spray.http.ContentType
import spray.http.MediaType
import spray.http.MediaTypes._
import spray.json._


trait SprayJsonRiakValueConverter {

  // TODO: this is not as simple as it looked. Only RiakValue with a JSON contentType should be allowed
  //       Is there a type-based solution or should we use runtime detection and use Try[T] as the output of a read?

  implicit def converter[T: RootJsonFormat: ClassTag] = new RiakValueConverter[T] {
    def read(riakValue: RiakValue): Try[T] = {
      riakValue.contentType match {
        case ContentType(`application/json`, _) => {
          val jsonFormat = implicitly[RootJsonFormat[T]]

          Try(jsonFormat.read(riakValue.value.asJson)) recoverWith {
            case e => Failure(ConversionFailedException(
                        "The JSON contained in the RiakValue cannot be used to produce an instance of %s".format(
                          implicitly[ClassTag[T]].runtimeClass.getSimpleName), e))
          }
        }

        case _ => Failure(
          new ConversionFailedException(
            "The ContentType of the riakValue is `%s` instead of the `application/json` required by SprayJsonConverter".format(
              riakValue.contentType, implicitly[ClassTag[T]].runtimeClass)))
      }
    }

    def write(obj: T): RiakValue = {
      RiakValue(implicitly[RootJsonFormat[T]].write(obj).compactPrint, ContentType.`application/json`)
    }
  }
}

object SprayJsonRiakValueConverter extends SprayJsonRiakValueConverter
