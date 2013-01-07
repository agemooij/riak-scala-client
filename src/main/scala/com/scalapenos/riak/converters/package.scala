package com.scalapenos.riak

import annotation.implicitNotFound


package object converters {

  /**
    * Provides the RiakValue deserialization for type T.
   */
  @implicitNotFound(msg = "Cannot find RiakValueReader or RiakValueConverter type class for ${T}")
  trait RiakValueReader[T] {
    def read(value: RiakValue): T
  }

  object RiakValueReader {
    implicit def func2Reader[T](f: RiakValue => T): RiakValueReader[T] = new RiakValueReader[T] {
      def read(value: RiakValue) = f(value)
    }
  }

  /**
    * Provides the RiakValue serialization for type T.
   */
  @implicitNotFound(msg = "Cannot find RiakValueWriter or RiakValueConverter type class for ${T}")
  trait RiakValueWriter[T] {
    def write(obj: T): RiakValue
  }

  object RiakValueWriter {
    implicit def func2Writer[T](f: T => RiakValue): RiakValueWriter[T] = new RiakValueWriter[T] {
      def write(obj: T) = f(obj)
    }
  }

  /**
    * Provides the RiakValue deserialization and serialization for type T.
   */
  trait RiakValueConverter[T] extends RiakValueReader[T] with RiakValueWriter[T]

}
