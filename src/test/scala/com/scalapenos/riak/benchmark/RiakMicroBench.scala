package com.scalapenos.riak
package benchmark

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random

import akka.actor._

import domain._


object RiakMicroBenchmark extends App {
  implicit val system = ActorSystem("micro-benchmark")

  import system.dispatcher

  val random = new Random()
  def randomUserId = random.nextLong().abs.toString
  def randomRoute = NavRoute(
    if (random.nextBoolean) Some(Location(random.nextDouble, random.nextDouble))
    else None
  )

  runMicroBenchMark("Java (blocking)", new BlockingJavaRiakDriver())
  runMicroBenchMark("Java (non-blocking)", new NonBlockingJavaRiakDriver())

  runMicroBenchMark("Java (blocking pbc)", new BlockingJavaProtobufRiakDriver())
  // runMicroBenchMark("Java (non-blocking pbc)", new NonBlockingJavaProtobufRiakDriver())

  runMicroBenchMark("Scala (spray-client)", SprayClientRiakDriver())

  system.shutdown


  private def runMicroBenchMark(label: String, driver: RiakDriver) {
    val bucket = driver.bucket[NavRoute]("ActiveRoutes")

    val start = System.currentTimeMillis

    Await.result(
      Future.sequence(List.fill(500)(runBasicCrudOperationsOnRandomKey(bucket))),
      40 seconds)

    val duration = System.currentTimeMillis - start

    driver.shutdown

    println(label + " took " + duration + " ms.")
  }

  private def runBasicCrudOperationsOnRandomKey(bucket: RiakBucket[NavRoute]): Future[List[Any]] = {
    val key = randomUserId
    val value1 = randomRoute
    val value2 = randomRoute

    Future.sequence(List(
      bucket.get(key),
      bucket.put(key, value1),
      bucket.get(key),
      bucket.put(key, value2),
      bucket.get(key),
      bucket.delete(key),
      bucket.get(key)
    ))
  }
}
