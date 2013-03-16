/*
 * Copyright (C) 2012-2013 Age Mooij (http://scalapenos.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scalapenos.riak.examples.albums

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import akka.actor._
import akka.testkit._
import spray.util._

import com.scalapenos.riak._

import AlbumRepositoryProtocol._


class RiakAlbumPepositorySpec extends Specification with NoTimeConversions {
  val timeout = 2 seconds

  // val activeRoute1 = Album(Some(Location(2001000, 42000000)))
  // val activeRoute2 = Album(Some(Location(5005000, 42424242)))


  "When receiving a FetchAlbumByTitle message, the RiakAlbumPepository" should {
    "reply with a Some[Album] if the active route existed" in new AkkaTestkitContext {
      val activeRouteStore = TestActorRef(Props[RiakAlbumPepository])
      val userId = randomUUID

      storeAlbumInDatabase(userId, activeRoute1)

      within(timeout) {
        activeRouteStore ! FetchAlbumByTitle(userId)

        val activeRouteFromDb = expectMsgType[Option[RiakMeta[Album]]]

        activeRouteFromDb must beSome[RiakMeta[Album]]
        activeRouteFromDb.get.data must beEqualTo(activeRoute1)
      }

      removeAlbumFromDatabase(userId)
    }

    "reply with a None if the active route doesn't exist" in new AkkaTestkitContext {
      val activeRouteStore = TestActorRef(Props[RiakAlbumPepository])
      val userId = randomUUID

      verifyAlbumDoesNotExistInDatabase(userId)

      within(timeout) {
        activeRouteStore ! FetchAlbumByTitle(userId)

        val activeRouteFromDb = expectMsgType[Option[RiakMeta[Album]]]

        activeRouteFromDb must beNone
      }
    }
  }

  "when receiving an UpdateAlbum message, the RiakAlbumPepository" should {
    "update the active route in the database if it already existed" in new AkkaTestkitContext {
      val activeRouteStore = TestActorRef(Props[RiakAlbumPepository])
      val userId = randomUUID

      storeAlbumInDatabase(userId, activeRoute1)

      within(timeout) {
        activeRouteStore ! UpdateAlbum(userId, activeRoute2)

        val activeRouteFromDb = expectMsgType[Option[RiakMeta[Album]]]

        activeRouteFromDb must beSome[RiakMeta[Album]]
        activeRouteFromDb.get.data must beEqualTo(activeRoute2)

        verifyAlbumExistsInDatabase(userId, activeRoute2)
      }

      removeAlbumFromDatabase(userId)
    }

    "create the active route in the database if it did not already exist" in new AkkaTestkitContext {
      val activeRouteStore = TestActorRef(Props[RiakAlbumPepository])
      val userId = randomUUID

      verifyAlbumDoesNotExistInDatabase(userId)

      within(timeout) {
        activeRouteStore ! UpdateAlbum(userId, activeRoute2)

        val activeRouteFromDb = expectMsgType[Option[RiakMeta[Album]]]

        activeRouteFromDb must beSome[RiakMeta[Album]]
        activeRouteFromDb.get.data must beEqualTo(activeRoute2)

        verifyAlbumExistsInDatabase(userId, activeRoute2)
      }

      removeAlbumFromDatabase(userId)
    }
  }

  // TODO: when the riak client supports getting all keys for a bucket we can implement better cleaning
  //step(removeAllAlbumsFromTheDatabase)


  // ==========================================================================
  // Test Helpers
  // ==========================================================================

  private def storeAlbumInDatabase(userId: UUID, route: Album)(implicit system: ActorSystem) {
    bucket.store(userId.toString, route).await

    verifyAlbumExistsInDatabase(userId, route)
  }

  private def removeAlbumFromDatabase(userId: UUID)(implicit system: ActorSystem) {
    bucket.delete(userId.toString).await

    verifyAlbumDoesNotExistInDatabase(userId)
  }

  private def verifyAlbumExistsInDatabase(userId: UUID, expectedRoute: Album)(implicit system: ActorSystem) {
    val dbValue = bucket.fetch(userId.toString).await

    dbValue must beSome[RiakValue]

    val routeInDb = dbValue.get.as[Album]

    routeInDb must beEqualTo(expectedRoute)
  }

  private def verifyAlbumDoesNotExistInDatabase(userId: UUID)(implicit system: ActorSystem) {
    bucket.fetch(userId.toString).await must beNone
  }

  private def bucket(implicit system: ActorSystem) = RiakClient(system).bucket("albums")
}


