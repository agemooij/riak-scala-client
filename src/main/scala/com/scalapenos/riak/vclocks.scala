package com.scalapenos.riak


// trait VClock

// case class VClocked[T](value: T, vclock: VClock) {
//   // ??
//   //def filter[A](f: T => Boolean): VClocked[A]

//   def map[A](f: T => A): VClocked[A] = VClocked(f(value), vclock)
//   def flatMap[A](f: (T, VClock) => VClocked[A]): VClocked[A] = f(value)
// }
