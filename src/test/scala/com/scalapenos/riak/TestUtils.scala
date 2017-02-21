package com.scalapenos.riak

import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap

import scala.concurrent.Future
import scala.collection.JavaConversions._

import akka.actor._
import akka.testkit._
import com.typesafe.config.{ Config, ConfigFactory }

import org.specs2.mutable._
import org.specs2.execute.{ Failure, FailureException }
import org.specs2.specification.StandardFragments.{ Backtab, Br }
import org.specs2.specification.{ Fragments, Step, Text }
import org.specs2.time.NoTimeConversions

trait AkkaActorSystemSpecification extends Specification with NoTimeConversions {
  implicit val defaultSystem = createActorSystem()

  // manual pimped future stolen^H^Hborrowed from spray.util because a
  // spray.util._ import causes implicit resolution conflicts with the above implicit actor system
  implicit def pimpFuture[T](fut: Future[T]): spray.util.pimps.PimpedFuture[T] = new spray.util.pimps.PimpedFuture[T](fut)

  def failTest(msg: String) = throw new FailureException(Failure(msg))

  lazy val actorSystems: ConcurrentHashMap[String, ActorSystem] = new ConcurrentHashMap[String, ActorSystem]()

  /* Add a final step to the list of test fragments that shuts down the actor system. */
  override def map(fs: ⇒ Fragments) =
    super.map(fs).add(Step(actorSystems.values().foreach(TestKit.shutdownActorSystem(_))))

  protected def createActorSystem(customConfig: Option[Config] = None): ActorSystem = {
    val systemName = s"tests-${randomUUID()}"
    val system = customConfig match {
      case Some(config) ⇒ ActorSystem(systemName, config)
      case None         ⇒ ActorSystem(systemName)
    }
    actorSystems.put(systemName, system)
    system
  }

  protected def decorateWith(fs: ⇒ Fragments)(text: String, block: ⇒ Unit) = {
    Seq(Br(), Br(), Text(text), Step(block)) ++: fs.middle :+ Backtab(1)
  }

  protected def createRiakClient(enableCompression: Boolean) = {
    val config = ConfigFactory.parseString(
      s"""
         |{
         | riak {
         |   enable-http-compression = $enableCompression
         | }
         |}
      """.stripMargin)

    RiakClient(createActorSystem(Some(config)))
  }
}

abstract class RiakClientSpecification extends AkkaActorSystemSpecification with Before {
  var client: RiakClient = _

  def before {
    client = RiakClient(defaultSystem)
  }

  skipAllUnless(RiakClient(defaultSystem).ping.await)

  private def specsWithParametrizedCompression(fs: ⇒ Fragments): Fragments = {
    Seq(true, false).map { enableCompression ⇒
      val compressionCaseText = s"When compression is ${if (enableCompression) "enabled" else "disabled"} in"

      fs.copy(middle = decorateWith(fs)(text = compressionCaseText, block = {
        client = createRiakClient(enableCompression)
      }))
    }.reduce(_ ^ _)
  }

  override def map(fs: ⇒ Fragments) = super.map(specsWithParametrizedCompression(fs))
}

trait RandomKeySupport {
  import java.util.UUID._

  def randomKey = randomUUID().toString
}

trait RandomBucketSupport { self: RandomKeySupport ⇒

  def randomBucket(client: RiakClient) = client.bucket("riak-bucket-tests-" + randomKey)
}
