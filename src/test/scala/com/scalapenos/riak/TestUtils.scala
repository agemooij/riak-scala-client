package com.scalapenos.riak

import org.specs2.mutable._

import akka.actor._
import akka.testkit._

abstract class AkkaTestkitContext extends TestKit(ActorSystem()) with ImplicitSender with After {
  def after = system.shutdown()
}
