package com.scalapenos.riak.converters

import spray.json._


trait SprayJsonConverter {

  // TODO: this is not as simple as it looked. Only RiakValue with a JSON contentType should be allowed
  //       Is there a type-based solution or should we use runtime detection and use Try[T] as the output of a read?

  // implicit object converter = new RiakValueConverter[T: RootJsonFormat] {
  //   def read(value: RiakValue): T = {
  //     val jsonFormat = implicitly[RootJsonFormat[T]]

  //   }

  //   def write(obj: T): RiakValue = {

  //   }
  // }
}

object SprayJsonConverter extends SprayJsonConverter
