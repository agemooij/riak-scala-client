package com.scalapenos.riak

import org.specs2.mutable._

import scala.concurrent.Future
import akka.actor._
import akka.testkit._

import org.specs2.execute.{ Failure, FailureException }
import org.specs2.specification.{ Fragments, Step }
import org.specs2.time.NoTimeConversions

trait AkkaActorSystemSpecification extends Specification with NoTimeConversions {
  implicit val system = ActorSystem("tests")

  // manual pimped future stolen^H^Hborrowed from spray.util because a
  // spray.util._ import causes implicit resolution conflicts with the above implicit actor system
  implicit def pimpFuture[T](fut: Future[T]): spray.util.pimps.PimpedFuture[T] = new spray.util.pimps.PimpedFuture[T](fut)

  def failTest(msg: String) = throw new FailureException(Failure(msg))

  /* Add a final step to the list of test fragments that shuts down the actor system. */
  override def map(fs: ⇒ Fragments) = super.map(fs).add(Step(system.shutdown))
}

trait RiakClientSpecification extends AkkaActorSystemSpecification with Before {
  var client: RiakClient = _

  def before {
    client = RiakClient(system)
  }

  skipAllUnless(RiakClient(system).ping.await)
}

trait RandomKeySupport {
  import java.util.UUID._

  def randomKey = randomUUID().toString
}

trait RandomBucketSupport {
  self: RiakClientSpecification with RandomKeySupport ⇒

  def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)
}
