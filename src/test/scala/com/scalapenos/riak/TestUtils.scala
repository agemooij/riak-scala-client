package com.scalapenos.riak

import org.specs2.mutable._

import scala.concurrent.Future
import akka.actor._
import akka.testkit._


abstract class AkkaTestkitContext extends TestKit(ActorSystem()) with ImplicitSender with After {
  def after = system.shutdown()
}


import org.specs2.execute.{Failure, FailureException}
import org.specs2.specification.{Fragments, Step}
import org.specs2.time.NoTimeConversions

trait AkkaActorSystemSpecification extends Specification with NoTimeConversions {
  implicit val system = ActorSystem(actorSystemNameFrom(getClass))
  implicit val executor = system.dispatcher

  // manual pimped future stolen from spray.util because a spray.util._ import causes implicit conflicts with the above implicit system
  implicit def pimpFuture[T](fut: Future[T]): spray.util.pimps.PimpedFuture[T] = new spray.util.pimps.PimpedFuture[T](fut)

  def failTest(msg: String) = throw new FailureException(Failure(msg))

  /* Add a final step to the list of test fragments that shuts down the actor system. */
  override def map(fs: => Fragments) = super.map(fs).add(Step(system.shutdown))

  private def actorSystemNameFrom(clazz: Class[_]) = clazz.getName.replace('.', '-').filter(_ != '$')
}

trait RiakClientSpecification extends AkkaActorSystemSpecification with Before {
  var client: RiakClient = _
  var connection: RiakConnection = _

  def before {
    client = RiakClient(system)
    connection = client.connect()
  }
}

trait RandomKeySupport {
  import java.util.UUID._

  def randomKey = randomUUID().toString
}
