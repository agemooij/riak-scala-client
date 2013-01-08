package com.scalapenos.riak
package converters

import scala.reflect.ClassTag
import scala.util._

import spray.http.ContentType
import spray.http.MediaType
import spray.http.MediaTypes._
import spray.json._


trait SprayJsonConverter {

  // TODO: this is not as simple as it looked. Only RiakValue with a JSON contentType should be allowed
  //       Is there a type-based solution or should we use runtime detection and use Try[T] as the output of a read?

  implicit def converter[T: RootJsonFormat: ClassTag] = new RiakValueConverter[T] {
    def read(value: RiakValue): Try[T] = {
      def extract(value: RiakValue): Try[T] = {
        val jsonFormat = implicitly[RootJsonFormat[T]]

        Try(jsonFormat.read(value.asString.asJson)) match {
          case s: Success[T] => s
          case Failure(e) =>
            Failure(ConversionFailedException(
              "The JSON contained in the RiakValue cannot be used to produce an instance of %s".format(
                implicitly[ClassTag[T]].runtimeClass.getSimpleName), e))
        }
      }

      value.contentType match {
        case ContentType(`application/json`, _) => extract(value)
        case _ => Failure(
          new ConversionFailedException(
            "The ContentType of the riakValue is `%s` instead of the `application/json` required by SprayJsonConverter".format(
              value.contentType, implicitly[ClassTag[T]].runtimeClass)))
      }
    }

    def write(obj: T): RiakValue = {
      throw new RuntimeException("Not implemented yet!")
    }
  }
}

object SprayJsonConverter extends SprayJsonConverter
