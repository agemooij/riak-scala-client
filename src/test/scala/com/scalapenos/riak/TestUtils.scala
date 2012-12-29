package com.scalapenos.riak

import org.specs2.mutable._
import org.specs2.specification.{Fragments, Step}
import org.specs2.time.NoTimeConversions

import akka.actor._
import akka.testkit._

abstract class AkkaTestkitContext extends TestKit(ActorSystem()) with ImplicitSender with After {
  def after = system.shutdown()
}


trait AkkaActorSystemSpecification extends Specification with NoTimeConversions {
  implicit val system = ActorSystem(actorSystemNameFrom(getClass))
  implicit val executor = system.dispatcher

  /* Add a final step to the list of test fragments that shuts down the actor system. */
  override def map(fs: => Fragments) = super.map(fs).add(Step(system.shutdown))

  private def actorSystemNameFrom(clazz: Class[_]) = clazz.getName.replace('.', '-').filter(_ != '$')
}


trait Specs2FutureMatchers {
  import org.specs2.matcher.Matcher
  import org.specs2.matcher.Matchers._
  import scala.concurrent._



}

