package com.scalapenos.riak
package converters

import scala.util._


trait BasicRiakValueConverters {

  implicit def stringRiakValueConverter = new RiakValueConverter[String] {
    def read(value: RiakValue): Try[String] = Success(value.asString)
    def write(obj: String): RiakValue = RiakValue(obj)
  }
}

object BasicRiakValueConverters extends BasicRiakValueConverters
