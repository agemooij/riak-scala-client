package com.scalapenos.riak

import scala.concurrent.Future
import scala.util._

import akka.actor._

import spray.json._

import com.basho.riak.client._
import com.basho.riak.client.bucket.Bucket
import com.basho.riak.client.builders.RiakObjectBuilder
import com.basho.riak.client.cap.VClock
import com.basho.riak.client.convert._
import com.basho.riak.client.http.util.Constants

import domain._


abstract class RiakDriverDataStore extends Actor with ActorLogging {
  import DataStore._
  import context.dispatcher

  implicit val system = context.system

  private val ActiveRoutesBucketName = "active-routes"

  private var driver: RiakDriver = null
  private var activeRoutes: RiakBucket[NavRoute] = null

  def initializeRiakDriver: RiakDriver

  override def preStart() {
    log.info("Starting the RiakJavaDriverDataStore...")

    driver = initializeRiakDriver
    activeRoutes = driver.bucket[NavRoute](ActiveRoutesBucketName)
  }

  override def postStop() {
    log.info("Closing connection to Riak...")

    driver.shutdown
  }

  def receive = {
    case FindActiveRoute(userId) => findActiveRoute(userId, sender)
    case UpdateActiveRoute(userId, route) => updateActiveRoute(userId, route, sender)
    case DeleteActiveRouteFor(userId) => deleteActiveRouteFor(userId, sender)
  }


  // ==========================================================================
  // Implementation Details
  // ==========================================================================

  private def findActiveRoute(userId: String, respondTo: ActorRef) {
    log.debug("Finding active route for user(%s)...".format(userId))

    activeRoutes.get(userId).onComplete {
      case Success(activeRouteOption) => respondTo ! FoundActiveRoute(userId, activeRouteOption)
      case Failure(failure) => log.error(failure, ""); respondTo ! DataStoreError(failure)
    }
  }

  private def updateActiveRoute(userId: String, route: NavRoute, respondTo: ActorRef) {
    log.debug("Updating active route for user(%s)...".format(userId))

    activeRoutes.put(userId, route).onComplete {
      case Success(stored) => respondTo ! UpdatedActiveRoute(userId, stored)
      case Failure(failure) => log.error(failure, ""); respondTo ! DataStoreError(failure)
    }
  }

  private def deleteActiveRouteFor(userId: String, respondTo: ActorRef) {
    log.debug("Deleting active route for user(%s)...".format(userId))

    activeRoutes.delete(userId).onComplete {
      case Success(_) => respondTo ! DeletedActiveRouteFor(userId)
      case Failure(failure) => log.error(failure, ""); respondTo ! DataStoreError(failure)
    }
  }
}

